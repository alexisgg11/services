param([Parameter(Mandatory=$true)][string]$BaseUrl)
$ErrorActionPreference = 'Stop'
function Assert-True($Condition, [string]$Message) { if (-not $Condition) { throw $Message } }
function Json-Body([string]$Form) {
    $data = @{}
    foreach ($part in $Form.Split('&')) {
        $pair = $part.Split('=',2)
        if ($pair.Length -eq 2) { $data[$pair[0]] = [Uri]::UnescapeDataString($pair[1].Replace('+',' ')) }
    }
    return ConvertTo-Json -InputObject $data -Compress
}
function Status([string]$Path, [string]$Method = 'GET', [string]$Body = '') {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api$Path" -Method $Method -Body (Json-Body $Body) -ContentType 'application/json' -SkipHttpErrorCheck
    return [int]$response.StatusCode
}
$health = Invoke-RestMethod "$BaseUrl/api/health"
Assert-True ($health.estado -eq 'ok') 'PostgreSQL no está saludable.'
$before = Invoke-RestMethod "$BaseUrl/api/productos?tamano=100"
$catalogs = Invoke-RestMethod "$BaseUrl/api/catalogos"
Assert-True ($catalogs.unidades.Count -eq 3) 'Se esperaban tres unidades.'
$max = Invoke-RestMethod "$BaseUrl/api/servicios/producto-mas-caro"
Assert-True ($max.precio -eq ($before.items | Measure-Object precio -Maximum).Maximum) 'Mayor costo incorrecto.'
$bySupplier = Invoke-RestMethod "$BaseUrl/api/servicios/por-proveedor?id=1"
Assert-True (@($bySupplier.items | Where-Object idProveedor -ne 1).Count -eq 0) 'El servicio mezcló proveedores.'
$byType = Invoke-RestMethod "$BaseUrl/api/servicios/por-tipo?id=5"
Assert-True (@($byType.items | Where-Object idTipo -ne 5).Count -eq 0) 'El servicio mezcló tipos.'
Assert-True ((Status '/productos?tamano=101') -eq 400) 'Tamaño excesivo aceptado.'
Assert-True ((Status '/productos?pagina=0') -eq 400) 'Página inválida aceptada.'
Assert-True ((Status '/productos?paginas=6') -eq 400) 'Nombre de parámetro incorrecto aceptado.'
Assert-True ((Status '/catalogos?pagina=1') -eq 400) 'Parámetro no admitido por catálogos aceptado.'
Assert-True ((Status '/servicios/por-tipo?id=1&buscar=Harina') -eq 400) 'Parámetro de otra operación aceptado.'
$page = Invoke-RestMethod "$BaseUrl/api/productos?pagina=2&tamano=5"
Assert-True ($page.items.Count -eq 5 -and $page.pagina -eq 2 -and $page.total -eq $before.total) 'Paginación incorrecta.'
$noMatch = Invoke-RestMethod "$BaseUrl/api/productos?buscar=%27%20OR%201%3D1%20--"
Assert-True ($noMatch.total -eq 0) 'La entrada de búsqueda modificó la consulta SQL.'
$outside = Invoke-RestMethod "$BaseUrl/api/productos?pagina=1000000&tamano=10"
Assert-True ($outside.pagina -eq $outside.paginas) 'La página fuera de rango no fue ajustada.'
$summary = Invoke-RestMethod "$BaseUrl/api/resumen"
Assert-True ($summary.productos -eq $before.total) 'Resumen incorrecto.'
Assert-True ((Status '/servicios/por-tipo?id=0') -eq 400) 'ID inválido aceptado.'
Assert-True ((Status '/productos/2147483647') -eq 404) 'Producto inexistente aceptado.'
Assert-True ((Status '/productos?buscar=Harina&buscar=Sal') -eq 400) 'Parámetros duplicados aceptados.'
$invalidJsonBodies = @(
    '{"descripcion":"Prueba","descripcion":"Otra","precio":"4.80","idTipo":"1","idProveedor":"1","idUnidad":"1"}',
    '{"descripcion":"Prueba","precio":"4.80","idTipo":"1","idProveedor":"1","idUnidad":"1","extra":"no"}'
    '{"descripcion":"Prueba","precio":"4.80","idTipo":"1","idProveedor":"1","idUnidad":"1","estado":false}'
)
foreach ($body in $invalidJsonBodies) {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api/productos/2147483647" -Method PUT -Body $body -ContentType 'application/json' -SkipHttpErrorCheck
    Assert-True ($response.StatusCode -eq 400) 'JSON con campos repetidos o desconocidos aceptado.'
}
Assert-True ((Status '/productos' 'POST' 'descripcion=Prueba&precio=1.001&idTipo=1&idProveedor=1&idUnidad=1') -eq 400) 'Precio con tres decimales aceptado.'
Assert-True ((Status '/productos' 'POST' 'descripcion=Prueba&precio=1.00&idTipo=999999&idProveedor=1&idUnidad=1') -eq 409) 'Referencia inexistente aceptada.'
$createdId = $null
try {
    $created = Invoke-RestMethod "$BaseUrl/api/productos" -Method POST -ContentType 'application/json' -Body (Json-Body 'descripcion=Prueba+API+temporal&precio=4.80&idTipo=1&idProveedor=1&idUnidad=1')
    $createdId = $created.idProducto
    $product = Invoke-RestMethod "$BaseUrl/api/productos/$createdId"
    Assert-True ($product.precio -eq 4.80) 'El decimal perdió precisión.'
    Assert-True ($product.estado -eq $true) 'El producto nuevo no está activo.'
    $null = Invoke-RestMethod "$BaseUrl/api/productos/$createdId" -Method PUT -ContentType 'application/json' -Body (Json-Body 'descripcion=Prueba+API+actualizada&precio=5.25&idTipo=2&idProveedor=2&idUnidad=1')
    $updated = Invoke-RestMethod "$BaseUrl/api/productos/$createdId"
    Assert-True ($updated.precio -eq 5.25 -and $updated.idProveedor -eq 2 -and $updated.idTipo -eq 2) 'Actualización incorrecta.'
    $search = Invoke-RestMethod "$BaseUrl/api/productos?buscar=Prueba%20API%20actualizada"
    Assert-True ($search.total -eq 1 -and $search.items[0].idProducto -eq $createdId) 'Búsqueda incorrecta.'
    $null = Invoke-RestMethod "$BaseUrl/api/productos/$createdId" -Method PUT -ContentType 'application/json' -Body (Json-Body 'descripcion=Prueba+API+actualizada&precio=99999999.99&idTipo=2&idProveedor=2&idUnidad=1')
    if ($max.precio -lt 99999999.99) {
        $temporalMax = Invoke-RestMethod "$BaseUrl/api/servicios/producto-mas-caro"
        Assert-True ($temporalMax.idProducto -eq $createdId) 'El servicio no reconoce el producto temporal más caro.'
    }
} finally {
    if ($null -ne $createdId) { $null = Invoke-RestMethod "$BaseUrl/api/productos/$createdId" -Method DELETE }
}
$after = Invoke-RestMethod "$BaseUrl/api/productos"
Assert-True ($after.total -eq $before.total) 'La prueba dejó productos temporales.'
Assert-True ((Status "/productos/$createdId") -eq 404) 'El producto inactivo sigue accesible por ID.'
Assert-True ((Status "/productos/$createdId" 'PUT' 'descripcion=No+reactivar&precio=1.00&idTipo=1&idProveedor=1&idUnidad=1') -eq 404) 'PUT permite editar un producto inactivo.'
Assert-True ((Status "/productos/$createdId" 'DELETE') -eq 404) 'El producto inactivo se elimina nuevamente.'
$searchAfter = Invoke-RestMethod "$BaseUrl/api/productos?buscar=Prueba%20API%20actualizada"
Assert-True ($searchAfter.total -eq 0) 'La búsqueda incluye el producto inactivo.'
$supplierAfter = Invoke-RestMethod "$BaseUrl/api/servicios/por-proveedor?id=2&tamano=100"
$typeAfter = Invoke-RestMethod "$BaseUrl/api/servicios/por-tipo?id=2&tamano=100"
Assert-True (@($supplierAfter.items | Where-Object idProducto -eq $createdId).Count -eq 0) 'El servicio por proveedor incluye inactivos.'
Assert-True (@($typeAfter.items | Where-Object idProducto -eq $createdId).Count -eq 0) 'El servicio por tipo incluye inactivos.'
$summaryAfter = Invoke-RestMethod "$BaseUrl/api/resumen"
Assert-True ($summaryAfter.productos -eq $before.total) 'El resumen cuenta productos inactivos.'
Assert-True ($summaryAfter.masCaro.idProducto -ne $createdId) 'El resumen incluye el producto inactivo más caro.'
$maxAfter = Invoke-RestMethod "$BaseUrl/api/servicios/producto-mas-caro"
Assert-True ($maxAfter.idProducto -ne $createdId) 'El servicio de mayor precio incluye inactivos.'
Write-Output "API verificada: paginación, resumen, tres servicios y CRUD con eliminación lógica. Productos activos conservados: $($after.total). Producto de prueba inactivo: $createdId."
