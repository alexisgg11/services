# Cogas · Abastecimiento de panadería

Sistema de gestión de productos con Java 25, Spring Boot, Spring Data JPA/Hibernate y PostgreSQL local o Supabase. Incluye API REST e interfaz web. La base inicial contiene 50 productos y los catálogos de tipos, proveedores y unidades de medida.

- [Instalación](#instalación)
- [Ejecución](#ejecución)
- [CRUD desde el navegador](#probar-el-crud-desde-el-navegador)
- [Servicios de consulta](#servicios-de-consulta)
- [Validaciones](#validaciones)
- [Pruebas automáticas](#pruebas-automáticas)

## Instalación

**Requisitos:** JDK 25, Maven 3.9 o posterior y Windows PowerShell 5.1 o PowerShell 7. Configura `JAVA_HOME` y comprueba que `java -version` y `mvn -version` utilicen Java 25.

```powershell
git clone https://github.com/alexisgg11/services.git
cd services
```

Todos los comandos de arranque se ejecutan desde `services`, donde están `scripts`, `database` y `abastecimiento`.

### Preparar la base de datos

| Modalidad | Instalación nueva | Archivo de conexión |
| --- | --- | --- |
| PostgreSQL local | Ejecuta [00_crear_base.sql](database/00_crear_base.sql) sobre `postgres`. Después ejecuta [01_tablas_y_datos.sql](database/01_tablas_y_datos.sql) sobre `panaderia_abastecimiento`. | `abastecimiento/.env` |
| Supabase | Crea un proyecto y ejecuta [01_tablas_y_datos.sql](database/01_tablas_y_datos.sql) en SQL Editor. Utiliza la base `postgres` existente y **Connect → Session pooler**. | `abastecimiento/.env.supabase` |

**Si la base ya existe:** no recargues los datos iniciales. Aplica [02_eliminacion_logica.sql](database/02_eliminacion_logica.sql) si aún no tiene `estado`. La migración conserva los productos y puede repetirse sin reactivar inactivos. Aplícala en cada base que utilices.

Copia las plantillas si todavía no tienes los archivos privados:

```powershell
if (-not (Test-Path abastecimiento/.env)) {
    Copy-Item abastecimiento/.env.example abastecimiento/.env
}
if (-not (Test-Path abastecimiento/.env.supabase)) {
    Copy-Item abastecimiento/.env.supabase.example abastecimiento/.env.supabase
}
```

Edita el archivo de la modalidad que utilizarás:

| Variable | Local | Supabase |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/panaderia_abastecimiento` | `jdbc:postgresql://HOST_SESSION_POOLER:5432/postgres?sslmode=require` |
| `DB_USER` | Tu usuario de PostgreSQL | Usuario indicado por Session pooler |
| `DB_PASSWORD` | Contraseña de PostgreSQL | Contraseña de la base de Supabase |

Conserva la contraseña entre comillas, especialmente si contiene `#`, y sin codificación porcentual. Mantén los demás valores de la plantilla. Los archivos privados están excluidos de Git. Las variables del sistema tienen prioridad sobre esos archivos.

## Ejecución

Con PostgreSQL local encendido:

```powershell
.\scripts\run.ps1 -BaseDatos Local
```

Con PostgreSQL en Supabase:

```powershell
.\scripts\run.ps1 -BaseDatos Supabase
```

El script compila, ejecuta las pruebas e inicia el servidor. Abre [http://localhost:8080](http://localhost:8080). Mantén la terminal abierta y detén el servidor con **Ctrl + C** antes de volver a iniciarlo o cambiar de modalidad. Local y Supabase son bases separadas; sus datos no se sincronizan automáticamente.

Si aparece **«El archivo de Cogas está en uso»**, detén el servidor anterior antes de recompilar. Si falla la compilación, revisa el primer `[ERROR]` de Maven. Si falla la conexión, revisa las credenciales y que la base seleccionada esté disponible.

## Probar el CRUD desde el navegador

El servidor debe estar iniciado. Las consultas GET pueden abrirse desde la barra de direcciones. Para POST, PUT y DELETE utilizaremos **F12 → Console**, sin utilizar los formularios de Cogas.

### 1. Listar y buscar productos — GET

Escribe estas direcciones en el navegador:

| Consulta | URL |
| --- | --- |
| Listar productos | [http://localhost:8080/api/productos](http://localhost:8080/api/productos) |
| Buscar por una parte de la descripción | [http://localhost:8080/api/productos?buscar=hari](http://localhost:8080/api/productos?buscar=hari) |
| Segunda página, hasta 5 productos | [http://localhost:8080/api/productos?pagina=2&tamano=5](http://localhost:8080/api/productos?pagina=2&tamano=5) |
| Combinar búsqueda y página | [http://localhost:8080/api/productos?buscar=hari&pagina=1](http://localhost:8080/api/productos?buscar=hari&pagina=1) |
| Consultar tipos, proveedores y unidades | [http://localhost:8080/api/catalogos](http://localhost:8080/api/catalogos) |

`?` inicia los parámetros y `&` los separa. La búsqueda admite fragmentos de la descripción y no distingue mayúsculas de minúsculas.

| Parámetro de productos | Si se omite |
| --- | --- |
| `buscar` | Sin filtro por descripción |
| `proveedor` | Sin filtro por proveedor |
| `tipo` | Sin filtro por tipo |
| `pagina` | Página 1 |
| `tamano` | Hasta 10 productos por página |

Los filtros `proveedor` y `tipo` reciben identificadores existentes. Por ejemplo, `/api/productos?proveedor=1&tipo=1`. La respuesta contiene `items`, `total`, `pagina`, `tamano` y `paginas`. `total` y `paginas` consideran los filtros aplicados. **`paginas` es un campo de salida, no un parámetro:** enviarlo en la URL devuelve 400.

También puedes consultar desde la consola del navegador:

```javascript
var respuesta = await fetch('/api/productos?buscar=hari&pagina=1');
var datos = await respuesta.json();
console.log(respuesta.status, datos);
```

### 2. Insertar un producto — POST

Abre [http://localhost:8080/api/catalogos](http://localhost:8080/api/catalogos) y revisa `idTipo`, `idProveedor` e `idUnidad`. Después abre **F12 → Console** en esa misma pestaña. Los ejemplos utilizan el ID `1` de cada catálogo; reemplázalo por los identificadores que correspondan a tu producto.

**Ejecuta el POST una sola vez:** cada ejecución crea un producto real. Mantén esa pestaña abierta sin recargar mientras completas los ejemplos para conservar las variables.

```javascript
var respuesta = await fetch('/api/productos', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    descripcion: 'Harina de prueba CRUD',
    precio: '10.00',
    idTipo: '1',
    idProveedor: '1',
    idUnidad: '1'
  })
});

var datos = await respuesta.json();
console.log(respuesta.status, datos);
var idProducto = datos.idProducto;
```

Lo esperado es **201** y una respuesta con `idProducto` y `mensaje`. Continúa solo si el registro fue exitoso. La variable `idProducto` conserva el identificador generado para los siguientes ejemplos.

Comprueba el producto registrado:

```javascript
var respuesta = await fetch('/api/productos/' + idProducto);
var datos = await respuesta.json();
console.log(respuesta.status, datos);
```

También puedes abrir `http://localhost:8080/api/productos/ID_RECIBIDO` en otra pestaña, sustituyendo el marcador por el número real.

### 3. Editar el mismo producto — PUT

En la misma consola, ejecuta:

```javascript
var respuesta = await fetch('/api/productos/' + idProducto, {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    descripcion: 'Harina de prueba actualizada',
    precio: '12.50',
    idTipo: '1',
    idProveedor: '1',
    idUnidad: '1'
  })
});

var datos = await respuesta.json();
console.log(respuesta.status, datos);
```

Lo esperado es **200** y el mensaje de actualización. **PUT requiere los cinco campos**, aunque solo cambies la descripción o el precio. Conserva los valores actuales de los demás campos; si elegiste otros IDs en el POST, utiliza esos mismos IDs en el PUT. Repite la consulta GET del ejemplo anterior para verificar los cambios.

### 4. Eliminar el producto de prueba — DELETE

```javascript
var respuesta = await fetch('/api/productos/' + idProducto, {
  method: 'DELETE'
});

var datos = await respuesta.json();
console.log(respuesta.status, datos);
```

Lo esperado es **200** y el mensaje de eliminación. No se envía cuerpo JSON.

La eliminación es **lógica**: cambia `estado` de `true` a `false`, conservando la fila y su ID en PostgreSQL. El producto deja de aparecer en las búsquedas, la paginación, el resumen y los servicios. Consultarlo o editarlo por la API devuelve **404**. El backend administra `estado`; no lo envíes en POST ni PUT.

Puedes comprobar la fila conservada desde pgAdmin o SQL Editor de Supabase:

```sql
SELECT id_producto, descripcion, estado
FROM panaderia.producto
WHERE estado = false
ORDER BY id_producto;
```

### Identificar cada operación en el código

Las rutas están en [ProductoController.java](abastecimiento/src/main/java/com/alexis/controller/ProductoController.java). La clase tiene el prefijo `/api`:

| Operación | Método Java | Método HTTP y ruta |
| --- | --- | --- |
| Listar o buscar | `listar` | GET `/api/productos` |
| Consultar por ID | `obtener` | GET `/api/productos/{id}` |
| Consultar catálogos | `catalogos` | GET `/api/catalogos` |
| Insertar | `registrar` | POST `/api/productos` |
| Editar | `editar` | PUT `/api/productos/{id}` |
| Eliminar lógicamente | `eliminar` | DELETE `/api/productos/{id}` |

La barra de direcciones envía GET al navegar. En la consola, `fetch` selecciona el método mediante `method`; si se omite, usa GET. Spring identifica la operación por **método HTTP + ruta**.

En los ejemplos, `JSON.stringify` convierte el objeto JavaScript en texto JSON para enviarlo. `await respuesta.json()` lee e interpreta la respuesta recibida, sin realizar otra petición. `console.log(respuesta.status, datos)` muestra el estado HTTP y los datos juntos.

Para verificar la petición, abre **F12 → Network**, selecciónala y revisa **Headers → Request Method**, **Payload** para el JSON enviado y **Response** para el resultado.

## Servicios de consulta

| Servicio | URL de ejemplo |
| --- | --- |
| Producto activo de mayor precio | [http://localhost:8080/api/servicios/producto-mas-caro](http://localhost:8080/api/servicios/producto-mas-caro) |
| Productos por proveedor | [http://localhost:8080/api/servicios/por-proveedor?id=1](http://localhost:8080/api/servicios/por-proveedor?id=1) |
| Productos por tipo | [http://localhost:8080/api/servicios/por-tipo?id=1](http://localhost:8080/api/servicios/por-tipo?id=1) |

Las consultas por proveedor y tipo admiten `pagina` y `tamano`. Todos los servicios excluyen productos inactivos.

## Validaciones

- POST y PUT requieren `descripcion`, `precio`, `idTipo`, `idProveedor` e `idUnidad`.
- Descripción: de 1 a 150 caracteres, sin caracteres de control.
- Precio: de `0` a `99999999.99`, con punto decimal y hasta dos decimales.
- Tipo, proveedor y unidad: IDs positivos que existan en sus tablas. Una referencia inexistente impide guardar el producto.
- Página: de 1 a 1 000 000. Tamaño: de 1 a 100. Una página mayor que la última disponible se ajusta a la última válida.
- Se rechazan parámetros de URL desconocidos o repetidos, JSON inválido y campos JSON desconocidos o repetidos.

| HTTP | Resultado |
| --- | --- |
| 200 | Consulta, actualización o eliminación correctas |
| 201 | Producto registrado |
| 400 | Datos, parámetros o JSON inválidos |
| 404 | Producto inexistente o inactivo |
| 405 | Método HTTP no admitido para la ruta |
| 409 | Referencia inexistente o restricción de integridad |
| 413 | Solicitud demasiado grande |
| 415 | Formato incorrecto; POST/PUT requieren JSON |
| 503 | Base de datos no disponible |

## Pruebas automáticas

Con el servidor detenido y Java 25 configurado:

```powershell
mvn -f abastecimiento/pom.xml clean test
```

Con el servidor iniciado, ejecuta en otra terminal con PowerShell 7:

```powershell
pwsh -NoProfile -File .\scripts\test-api.ps1 -BaseUrl http://127.0.0.1:8080
```

El script verifica consultas, paginación, validaciones, servicios y CRUD. Su producto de prueba queda inactivo en PostgreSQL; el total de productos activos existentes se conserva.

## Estructura y documentación

| Carpeta | Responsabilidad |
| --- | --- |
| `abastecimiento/src/main/java/com/alexis/controller` | Endpoints REST y respuestas de error |
| `abastecimiento/src/main/java/com/alexis/dto` | Datos de entrada y salida de la API |
| `abastecimiento/src/main/java/com/alexis/service` | Validaciones, reglas y transacciones |
| `abastecimiento/src/main/java/com/alexis/repository` | Repositorios JPA y consultas |
| `abastecimiento/src/main/java/com/alexis/model` | Entidades y resultados internos |
| `abastecimiento/src/main/java/com/alexis/config` | Configuración, conexiones y validación de solicitudes |
| `abastecimiento/src/main/java/com/alexis/util` | Diagnósticos de arranque |
| `abastecimiento/src/main/resources/view` | Interfaz HTML, CSS y JavaScript |
| `database` | Instalación y migración de PostgreSQL |
| `scripts` | Arranque y pruebas |

Documentación: [API](docs/api.md) · [Arquitectura](docs/arquitectura.md) · [Supabase](docs/supabase-local.md) · [Despliegue](docs/despliegue.md).
