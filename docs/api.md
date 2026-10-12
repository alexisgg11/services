# API

Todas las rutas están bajo `/api`. Respuestas JSON; POST/PUT reciben `application/x-www-form-urlencoded`.

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

La API cambió del listado completo a una respuesta paginada; la interfaz y `scripts/test-api.ps1` consumen el contrato actual.
