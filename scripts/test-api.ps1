param([Parameter(Mandatory=$true)][string]$BaseUrl)
$ErrorActionPreference = 'Stop'
function Assert-True($Condition, [string]$Message) { if (-not $Condition) { throw $Message } }
function Status([string]$Path, [string]$Method = 'GET', [string]$Body = '') {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api$Path" -Method $Method -Body $Body -ContentType 'application/x-www-form-urlencoded' -SkipHttpErrorCheck
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
Assert-True ((Status '/productos' 'POST' 'descripcion=Prueba&precio=1.001&idTipo=1&idProveedor=1&idUnidad=1') -eq 400) 'Precio con tres decimales aceptado.'
Assert-True ((Status '/productos' 'POST' 'descripcion=Prueba&precio=1.00&idTipo=999999&idProveedor=1&idUnidad=1') -eq 409) 'Referencia inexistente aceptada.'
$createdId = $null
try {
    $created = Invoke-RestMethod "$BaseUrl/api/productos" -Method POST -ContentType 'application/x-www-form-urlencoded' -Body 'descripcion=Prueba+API+temporal&precio=4.80&idTipo=1&idProveedor=1&idUnidad=1'
    $createdId = $created.idProducto
    $product = Invoke-RestMethod "$BaseUrl/api/productos/$createdId"
    Assert-True ($product.precio -eq 4.80) 'El decimal perdió precisión.'
    $null = Invoke-RestMethod "$BaseUrl/api/productos/$createdId" -Method PUT -ContentType 'application/x-www-form-urlencoded' -Body 'descripcion=Prueba+API+actualizada&precio=5.25&idTipo=2&idProveedor=2&idUnidad=1'
    $updated = Invoke-RestMethod "$BaseUrl/api/productos/$createdId"
    Assert-True ($updated.precio -eq 5.25 -and $updated.idProveedor -eq 2 -and $updated.idTipo -eq 2) 'Actualización incorrecta.'
    $search = Invoke-RestMethod "$BaseUrl/api/productos?buscar=Prueba%20API%20actualizada"
    Assert-True ($search.total -eq 1 -and $search.items[0].idProducto -eq $createdId) 'Búsqueda incorrecta.'
} finally {
    if ($null -ne $createdId) { $null = Invoke-RestMethod "$BaseUrl/api/productos/$createdId" -Method DELETE }
}
$after = Invoke-RestMethod "$BaseUrl/api/productos"
Assert-True ($after.total -eq $before.total) 'La prueba dejó productos temporales.'
Write-Output "API verificada: paginación, resumen, consultas parametrizadas, tres servicios y CRUD. Productos conservados: $($after.total)."
