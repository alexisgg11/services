# API REST de Cogas

Todas las rutas están bajo `/api`. Respuestas JSON; POST/PUT reciben `application/json`, validado mediante `ProductoRequest` y Jakarta Validation. Las respuestas usan DTO y Jackson.

| Método y ruta | Resultado |
| --- | --- |
| GET /productos | Página del catálogo |
| GET /productos/{id} | Producto por ID |
| POST /productos | Crear; 201 e idProducto |
| PUT /productos/{id} | Editar |
| DELETE /productos/{id} | Eliminar |
| GET /catalogos | Tipos, proveedores y unidades |
| GET /resumen | Total, producto más caro y cantidades por proveedor |
| GET /servicios/producto-mas-caro | Mayor costo; en empate, menor ID |
| GET /servicios/por-proveedor?id=1 | Página filtrada por proveedor |
| GET /servicios/por-tipo?id=5 | Página filtrada por tipo |
| GET /health | Comprobación de conexión, tabla y permisos |

## Paginación

`productos`, `por-proveedor` y `por-tipo` reciben `pagina` (1 por defecto, hasta 1.000.000) y `tamano` (10 por defecto, entre 1 y 100). `productos` acepta además `buscar`, `proveedor` y `tipo`.

```json
{"items":[],"total":0,"pagina":1,"tamano":10,"paginas":1}
```

`total` considera los filtros. Una página mayor que la última se ajusta a la última válida. Un resultado vacío devuelve items vacío, total 0 y página 1 de 1. Tamaños, IDs o páginas inválidos devuelven 400. No hay opción para descargar una lista ilimitada.

POST/PUT requieren `descripcion`, `precio`, `idTipo`, `idProveedor`, `idUnidad`. Descripción de 1 a 150 caracteres, sin caracteres de control. Precio entre 0 y 99999999.99, con punto decimal y hasta dos decimales; se rechazan comas, notación científica, signos y espacios. IDs enteros positivos y referencias existentes. Las mismas restricciones se aplican al registrar y editar, tanto en la interfaz como en el backend. Errores: 400 validación, 404 inexistente, 409 restricción SQL, 413 tamaño, 415 formato, 503 PostgreSQL no disponible.

La interfaz y `scripts/test-api.ps1` consumen JSON. Una solicitud inválida devuelve `error` y, cuando corresponde, `campos` con los mensajes de validación del DTO.

## Registro y edición con DTO

Ejemplo de cuerpo JSON para POST `/api/productos` o PUT `/api/productos/{id}`:

```json
{"descripcion":"Harina de trigo","precio":"4.80","idTipo":"1","idProveedor":"1","idUnidad":"1"}
```

Los valores de entrada son cadenas para conservar y validar el formato de precio e identificadores. Las respuestas devuelven IDs enteros y precio decimal. Crear responde 201 y encabezado `Location`; editar y eliminar responden 200. Un JSON mal formado, campos repetidos o desconocidos se rechazan con 400; un formato distinto de JSON se rechaza con 415.

`dto` define el contrato de entrada y salida; `model` contiene los objetos que usa el repositorio. El controlador transforma los modelos en DTO de respuesta. No se exponen credenciales ni conexiones de base de datos.

## Probar un POST sin interfaz ni Postman

Con el servidor iniciado, abre otra terminal de PowerShell y ejecuta:

```powershell
$json = @{
    descripcion = 'Producto de prueba JSON'
    precio = '4.80'
    idTipo = '1'
    idProveedor = '1'
    idUnidad = '1'
} | ConvertTo-Json

Invoke-RestMethod -Uri 'http://localhost:8080/api/productos' `
    -Method POST -ContentType 'application/json' -Body $json
```

Esto crea un producto real en la base seleccionada. Conserva el `idProducto` devuelto para consultarlo con GET `/api/productos/{id}` y eliminar el registro de prueba con DELETE a la misma ruta.

Escribir una URL en la barra del navegador envía GET. GET `/api/productos?buscar=harina` consulta; no inserta. El registro exige POST y el JSON en el cuerpo. Se conserva la separación entre lectura y escritura de la base inicial del docente. Los repositorios usan JPA/Hibernate; el contrato HTTP continúa siendo el mismo.
