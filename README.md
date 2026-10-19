# Cogas · Abastecimiento de panadería

Aplicación web con Java 25, Spring Boot 4.1.1, Spring Data JPA/Hibernate, API REST y PostgreSQL local o Supabase. Permite registrar, editar, eliminar y buscar productos, consultar proveedores, tipos y unidades, y ejecutar tres servicios: producto más caro, productos por proveedor y productos por tipo.

La base inicial contiene cuatro tablas y 50 productos. Los proveedores, tipos y unidades se consultan y seleccionan; no tienen formularios de registro o edición.

Las secciones 1 a 5 explican la instalación y las pruebas. La [guía de funcionamiento](#6-cómo-funciona-cogas) explica las capas, las tecnologías y el recorrido completo de una petición y su respuesta.

## 1. Preparar el equipo

- JDK 25 y Maven 3.9 o posterior. Configura `java` y `mvn` en el PATH y `JAVA_HOME` con la carpeta del JDK 25.
- Windows PowerShell 5.1 o PowerShell 7 para iniciar la aplicación. Las pruebas de API requieren PowerShell 7 (`pwsh`).
- PostgreSQL y pgAdmin si usarás la base local. Para Supabase necesitas Internet y un proyecto con PostgreSQL activo.

```powershell
java -version
mvn -version
```

Ambos comandos deben indicar Java 25.

Clona el repositorio y entra en su carpeta:

```powershell
git clone https://github.com/alexisgg11/services.git
cd services
```

Los comandos siguientes se ejecutan desde **services**, donde están `scripts`, `database` y `abastecimiento`. Si la terminal está dentro de `abastecimiento`, ejecuta `cd ..`.

## 2. Configurar la base local

1. En pgAdmin, abre Query Tool sobre la base `postgres` y ejecuta [database/00_crear_base.sql](database/00_crear_base.sql).
2. Abre Query Tool sobre la nueva base **panaderia_abastecimiento** y ejecuta [database/01_tablas_y_datos.sql](database/01_tablas_y_datos.sql).
3. Copia la plantilla solo si todavía no existe tu archivo privado:

   ```powershell
   if (-not (Test-Path abastecimiento/.env)) {
       Copy-Item abastecimiento/.env.example abastecimiento/.env
   }
   ```

4. Edita `abastecimiento/.env`:

   ```dotenv
   DB_URL=jdbc:postgresql://localhost:5432/panaderia_abastecimiento
   DB_USER=TU_USUARIO_POSTGRES
   DB_PASSWORD="TU_CONTRASEÑA"
   APP_HOST=127.0.0.1
   PORT=8080
   DB_POOL_SIZE=5
   HTTP_THREADS=8
   DB_QUERY_TIMEOUT_SECONDS=10
   DB_CONNECTION_TIMEOUT_SECONDS=20
   ```

Sustituye usuario y contraseña por los de PostgreSQL local. Conserva la contraseña entre comillas, especialmente si contiene `#`. No vuelvas a cargar los datos iniciales si ya tienes la base preparada.

## 3. Configurar Supabase

El backend y la interfaz siguen ejecutándose en tu equipo; solo PostgreSQL está en Supabase. Conserva el archivo `.env` y tu base local.

1. Crea tu proyecto en Supabase. Para una base nueva, ejecuta `database/01_tablas_y_datos.sql` en SQL Editor. No ejecutes `00_crear_base.sql`: Supabase ya tiene una base llamada `postgres`. Si ya migraste tus datos, omite este paso.
2. Copia la plantilla si el archivo privado todavía no existe:

   ```powershell
   if (-not (Test-Path abastecimiento/.env.supabase)) {
       Copy-Item abastecimiento/.env.supabase.example abastecimiento/.env.supabase
   }
   ```

3. En Supabase abre **Connect → Session pooler** y copia el host y usuario de tu proyecto. Edita `abastecimiento/.env.supabase`:

   ```dotenv
   DB_URL=jdbc:postgresql://HOST_SESSION_POOLER:5432/postgres?sslmode=require
   DB_USER=postgres.REFERENCIA_PROYECTO
   DB_PASSWORD="TU_CONTRASEÑA_DE_LA_BASE"
   APP_HOST=127.0.0.1
   PORT=8080
   DB_POOL_SIZE=5
   HTTP_THREADS=8
   DB_QUERY_TIMEOUT_SECONDS=10
   DB_CONNECTION_TIMEOUT_SECONDS=20
   ```

Usa la contraseña original de la base, entre comillas y sin codificación porcentual: JDBC la recibe por separado. No necesitas claves de la Data API. Los archivos `.env` y `.env.supabase` están excluidos de Git.

Para trasladar una base existente, sigue [la guía de Supabase](docs/supabase-local.md). Las bases local y Supabase no se sincronizan automáticamente. Las variables del sistema tienen prioridad sobre los archivos de configuración.

## 4. Iniciar y detener Cogas

**Con PostgreSQL local encendido:**

```powershell
.\scripts\run.ps1 -BaseDatos Local
```

**Con PostgreSQL en Supabase:**

```powershell
.\scripts\run.ps1 -BaseDatos Supabase
```

El script selecciona el archivo de conexión, compila, ejecuta las pruebas y arranca el JAR de Spring Boot. El JAR incluye las librerías necesarias y Tomcat embebido. No inicia PostgreSQL, no crea tablas ni vuelve a insertar los datos.

Abre [Cogas](http://127.0.0.1:8080/). Deja la terminal abierta mientras utilices el sistema. Si cambias `PORT`, usa ese puerto en el navegador.

Para detener el servidor, pulsa **Ctrl + C** en la terminal donde lo iniciaste. Para cambiar de Local a Supabase o al revés, detén el servidor y ejecuta el otro comando. Reinicia también cuando cambies código o configuración. No necesitas ejecutar el script por cada consulta.

## 5. Probar el sistema

### Pruebas del backend

Con el servidor detenido y `JAVA_HOME` apuntando al JDK 25:

```powershell
mvn -f abastecimiento/pom.xml clean test
```

Actualmente hay **31 pruebas** de configuración, validaciones, servicios con repositorios JPA, DTO y controladores REST con Spring MVC. El resultado esperado es `BUILD SUCCESS`, sin errores ni fallos.

### Pruebas de API

Inicia Cogas en el modo que quieras comprobar. En **otra terminal**, desde `services`, ejecuta:

```powershell
pwsh -NoProfile -File .\scripts\test-api.ps1 -BaseUrl http://127.0.0.1:8080
```

Comprueba conexión, paginación, búsqueda parametrizada, resumen, tres servicios y registro, edición y eliminación. Crea un producto temporal y lo elimina al terminar; conserva los productos existentes. El resultado esperado empieza con `API verificada`.

### Prueba desde la interfaz

1. Registra un producto con descripción, precio, tipo, proveedor y unidad. Comprueba el aviso de registro exitoso.
2. Edita ese producto y comprueba el aviso de actualización.
3. Prueba campos vacíos y precios inválidos: negativos, más de dos decimales, coma decimal o notación científica. Deben mostrarse errores y no guardarse los cambios.
4. Consulta el producto más caro y filtra por proveedor y tipo. Comprueba resultados, mensajes y paginación.
5. Elimina el producto de prueba: primero aparece una confirmación y después el aviso de eliminación.

La descripción admite entre 1 y 150 caracteres, sin caracteres de control. El precio admite de 0 a 99 999 999.99, con punto decimal y hasta dos decimales. Tipo, proveedor y unidad deben corresponder a registros existentes. Las restricciones se aplican al registrar y editar, en la interfaz y en el backend.

## Si el servidor no inicia

`BUILD SUCCESS` confirma la compilación; la conexión a la base y el servidor HTTP se comprueban después. Lee el diagnóstico que aparece a continuación:

- **Usuario o contraseña rechazados:** revisa el archivo del modo seleccionado (`.env` o `.env.supabase`).
- **Tiempo de conexión agotado:** comprueba que PostgreSQL local esté encendido o Supabase esté activo y accesible. La espera es de 20 segundos por defecto; puedes configurar `DB_CONNECTION_TIMEOUT_SECONDS` entre 3 y 60.
- **Puerto ocupado:** detén el servidor anterior con Ctrl + C o cambia `PORT` en el perfil seleccionado.
- **Falta el esquema o la tabla:** prepara esa base con `database/01_tablas_y_datos.sql`. No repitas la carga si las tablas y los datos ya existen.

## Estructura

```text
services/
  abastecimiento/
    .env.example              Plantilla para PostgreSQL local
    .env.supabase.example     Plantilla para Supabase
    pom.xml                   Dependencias y compilación con Java 25
    src/main/java/com/alexis/
      App.java                Arranque con SpringApplication y @SpringBootApplication
      config/                 Configuración y pool de conexiones
      model/                  Cuatro entidades JPA y resultados internos de consulta
      dto/                    Solicitudes y respuestas de la API REST
      controller/             @RestController y errores centralizados
      service/                @Service, validaciones y transacciones
      repository/             Interfaces JpaRepository y consultas JPQL parametrizadas
      util/                   Diagnósticos de arranque sin credenciales
    src/main/resources/
      application.properties  Configuración Spring; Hibernate valida las tablas
      view/                   Interfaz HTML, CSS y JavaScript
    src/test/java/com/alexis/  Pruebas del backend
  database/                   Creación de tablas y datos iniciales
  scripts/                    Arranque y pruebas de API
  docs/                       Arquitectura, API, comparación con el docente y despliegue
  Dockerfile                  Empaquetado para despliegue
  render.yaml                 Configuración de Render
```

- [Arquitectura](docs/arquitectura.md)
- [Comparación con la base del docente](docs/comparacion-docente.md)
- [API y servicios](docs/api.md)
- [Backend local con Supabase](docs/supabase-local.md)
- [Despliegue en Render](docs/despliegue.md)

Cogas es un monolito por capas con Spring Boot que expone una API REST. Spring crea e inyecta los componentes; Jackson convierte los DTO a JSON. El alcance actual no incluye autenticación, compras ni movimientos de stock; **Admin** es una etiqueta del menú.

La comparación con el docente toma como referencia el primer commit `6b0c54a` de SOAS-Servicio, enlazado en la evaluación. Nuestro proyecto mantiene Java 25. Hibernate usa `ddl-auto=validate`: no modifica las tablas ni carga ejemplos al arrancar.

## 6. Cómo funciona Cogas

Cogas es un **monolito por capas**: se compila y ejecuta como una aplicación Java, organizada por responsabilidades. La interfaz se entrega desde el mismo servidor, pero su JavaScript se ejecuta en el navegador. El backend se ejecuta en Java y la información persistente se almacena en PostgreSQL.

La API REST permite que la interfaz u otro cliente, como PowerShell, solicite operaciones mediante HTTP. **Consumir la API** significa enviar esas peticiones y utilizar las respuestas. Un **endpoint** es una operación accesible mediante un método HTTP y una ruta; por ejemplo, `POST /api/productos` registra y `GET /api/productos` consulta.

Los tres servicios de consulta son endpoints de esta aplicación. No son tres microservicios desplegados por separado. La carpeta `service` es la capa de lógica de negocio; la palabra «servicio» también se utiliza en el curso para las consultas que expone la API.

### Tres formas de comunicación

| Entre qué partes | Cómo se comunican | Qué intercambian |
| --- | --- | --- |
| Cliente y backend | Peticiones y respuestas HTTP | Método, URL, encabezados, estado HTTP y, cuando corresponde, texto JSON |
| Controlador, servicio y repositorio | Llamadas a métodos de objetos Java dentro del mismo proceso | DTO, entidades, identificadores, listas, páginas y excepciones |
| Backend y PostgreSQL | Hibernate utiliza JDBC y el controlador de PostgreSQL sobre las conexiones del pool | SQL con parámetros y resultados de la base |

El controlador no realiza otra petición HTTP para llamar al servicio. Tampoco envía JSON al repositorio: dentro de Java se trabaja con objetos y valores Java. JSON se utiliza en el límite HTTP de esta API.

## 7. Responsabilidad de cada capa

Las capas están separadas por responsabilidad y trabajan juntas; no son aplicaciones independientes.

| Parte | Responsabilidad en Cogas | Ejemplo |
| --- | --- | --- |
| `App.java` | Iniciar Spring y cargar el contexto de la aplicación | `SpringApplication.run(App.class, args)` |
| `config` | Cargar y validar configuración privada; preparar el pool, el DataSource y el servidor | `AppConfig`, `ConexionDB`, `SpringConfig` |
| `model` | Representar las entidades de la base y los resultados internos de consultas | `Producto`, `Proveedor`, `PaginaProductos`, `Resumen` |
| `dto` | Definir los datos de entrada y salida de la API | `ProductoRequest`, `ProductoResponse`, `OperacionResponse`, `ErrorResponse` |
| `controller` | Recibir las operaciones HTTP, llamar al servicio y preparar las respuestas | `ProductoController`; errores comunes en `ApiExceptionHandler` |
| `service` | Aplicar reglas de negocio y delimitar transacciones | Validar precio, guardar un producto o decidir qué hacer si no existe |
| `repository` | Acceder a los datos mediante Spring Data JPA | `ProductoRepository` y repositorios de los tres catálogos |
| `resources/view` | Presentar la información e interactuar con la API | HTML, JavaScript y CSS |
| `util` | Reunir utilidades de apoyo | `DiagnosticoArranque` presenta fallos de inicio sin credenciales |
| `database` | Preparar manualmente las tablas y los datos iniciales | Scripts SQL de instalación |
| `src/test` y `scripts/test-api.ps1` | Comprobar reglas, configuración, controladores y funcionamiento HTTP | Pruebas automatizadas y CRUD con un producto temporal |

Las cuatro entidades son `Producto`, `TipoProducto`, `Proveedor` y `UnidadMedida`. `PaginaProductos` y `Resumen` agrupan resultados internos; no son tablas nuevas. Un DTO tampoco necesita una tabla propia.

### Arranque e inyección por constructor

`App` tiene `@SpringBootApplication`. Al ejecutar `SpringApplication.run`, Spring Boot inicia el contexto, configura los componentes de la aplicación e inicia Tomcat, el servidor HTTP embebido. Spring detecta controladores, servicios, entidades y repositorios dentro de los paquetes de la aplicación.

Spring Data crea las implementaciones de las interfaces de repositorio. Spring proporciona esas dependencias al crear el servicio y luego proporciona el servicio al crear el controlador. Esto se llama **inyección de dependencias por constructor**.

Ejemplo real del controlador:

```java
private final ProductoService servicio;

public ProductoController(ProductoService servicio) {
    this.servicio = servicio;
}
```

El controlador guarda una referencia al objeto de servicio. Más adelante puede llamar a `servicio.masCaro()`; no busca el archivo del servicio ni crea otra aplicación para cada petición.

`AppConfig` lee el archivo de conexión seleccionado o las variables del entorno y valida sus valores. `ConexionDB` utiliza esa configuración para preparar HikariCP. Un **pool** mantiene y reutiliza conexiones: el backend toma una conexión cuando la necesita y la devuelve al terminar, en lugar de abrir una nueva para cada operación. El `DataSource` es el componente desde el cual se obtienen esas conexiones. Hibernate utiliza el DataSource configurado en Cogas.

## 8. Qué es cada tecnología

| Tecnología o concepto | Qué significa | Función en el proyecto |
| --- | --- | --- |
| Java y JDK | Lenguaje y herramientas de desarrollo y ejecución | Clases, objetos, métodos y ejecución del backend con Java 25 |
| Maven | Herramienta de construcción y gestión de dependencias | Lee `pom.xml`, descarga bibliotecas, compila, ejecuta pruebas y empaqueta el JAR |
| Spring Boot | Proyecto de Spring que facilita configurar y arrancar aplicaciones | Integra las bibliotecas y el servidor embebido |
| Spring MVC | Parte web de Spring | Identifica rutas y métodos HTTP, procesa parámetros y cuerpos, y genera respuestas mediante conversores |
| API REST | Interfaz HTTP para operar sobre recursos | Permite consultar y modificar productos con GET, POST, PUT y DELETE |
| JPA / Jakarta Persistence | Especificación y API estándar de persistencia para Java | Define anotaciones e interfaces para mapear entidades y gestionar su persistencia |
| Spring Data JPA | Biblioteca de Spring construida sobre JPA | Proporciona las implementaciones de repositorios y operaciones habituales |
| Hibernate ORM | Biblioteca que implementa JPA | Gestiona entidades, genera el SQL necesario y mapea resultados a objetos Java |
| JDBC | API estándar de acceso a bases de datos incluida en el JDK | Permite ejecutar SQL y trabajar con resultados a través de un controlador |
| Controlador JDBC de PostgreSQL | Biblioteca específica del motor | Implementa la comunicación entre JDBC y PostgreSQL |
| PostgreSQL | Motor de base de datos relacional | Ejecuta el SQL y almacena los registros |
| Supabase | Plataforma que aloja nuestra base PostgreSQL en la nube | Permite mantener el backend local usando una base remota |
| Jackson | Biblioteca independiente para procesar datos, integrada aquí con Spring MVC | Convierte JSON a DTO de entrada y DTO de salida a JSON |
| JSON | Formato de texto para intercambiar datos | Representa los datos del cuerpo de las peticiones y respuestas |
| DTO | Data Transfer Object: objeto para transferir datos | Define qué datos recibe o devuelve una operación de la API |

JPA pertenece al estándar Jakarta Persistence del ecosistema Java; no es exclusivo de Spring Boot ni viene incluido en el JDK. Su API, con anotaciones e interfaces, llega al proyecto como dependencia. Hibernate implementa ese estándar. Jackson también es una biblioteca independiente que se puede utilizar sin Spring Boot.

En nuestro `pom.xml` declaramos, entre otras dependencias:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

Este starter incorpora las dependencias de Spring Data JPA, la API de persistencia e Hibernate. Maven las descarga; Spring Boot las integra con la aplicación. La dependencia `spring-boot-starter-webmvc` aporta la integración web y JSON; el controlador PostgreSQL también está declarado en `pom.xml`.

### Qué significa ORM y persistencia

**ORM** significa mapeo objeto-relacional: relaciona clases con tablas, atributos con columnas y objetos con registros. Las anotaciones de JPA describen esa correspondencia. Spring Boot detecta las entidades al arrancar; las anotaciones indican cómo se relacionan con la base, no dónde están sus archivos.

Fragmento de nuestra entidad `Producto`, omitiendo otros campos y métodos:

```java
@Entity
@Table(name = "producto", schema = "panaderia")
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;
}
```

`@Entity` identifica una entidad; `@Table` indica su tabla; `@Id` señala la clave primaria; `@Column` describe una columna. `@ManyToOne` y `@JoinColumn` representan las relaciones de cada producto con su tipo, proveedor y unidad.

**Persistencia** significa conservar los datos más allá de la ejecución de la aplicación. Al confirmar el registro de un producto, PostgreSQL lo almacena y puede recuperarse después de cerrar y volver a abrir Cogas. Hibernate gestiona cómo guardar, recuperar, actualizar y eliminar esos datos desde objetos Java; PostgreSQL es quien los almacena.

### JDBC manual y JPA/Hibernate

Antes utilizábamos JDBC directamente: escribíamos SQL con parámetros, usábamos `PreparedStatement`, leíamos las filas mediante `ResultSet` y construíamos los objetos Java. No utilizábamos `JdbcTemplate`.

Ahora el repositorio extiende `JpaRepository<Producto, Integer>`. Spring Data aporta operaciones y consultas; Hibernate genera el SQL y lee los resultados. JDBC y el controlador de PostgreSQL siguen siendo necesarios por debajo. No se ha añadido una segunda base ni otra conexión de negocio paralela.

| Tarea | JDBC manual anterior | JPA/Hibernate actual |
| --- | --- | --- |
| Registrar y editar | Escribir INSERT/UPDATE y asignar sus parámetros | Preparar la entidad y usar `saveAndFlush` |
| Buscar por ID | Ejecutar SELECT y recorrer su resultado | Usar `findById` |
| Transformar filas a entidades | Construir cada objeto leyendo `ResultSet` | Hibernate realiza el mapeo |
| Consulta específica | SQL escrito en el repositorio | Método derivado o JPQL parametrizado |
| Conexiones y transacciones | Gestión explícita con JDBC | Integración de JPA y transacciones de Spring sobre el pool configurado |

Un `ResultSet` es un objeto JDBC que permite recorrer las filas devueltas por una consulta; no es un texto JSON. En nuestras consultas JPA es Hibernate quien trabaja con esos resultados, sin que el servicio recorra las filas manualmente. `ConexionDB` conserva una comprobación JDBC directa para verificar conectividad, esquema y permisos al iniciar.

JPQL consulta entidades y sus atributos, en lugar de escribir directamente nombres de tablas y columnas. Por ejemplo, nuestra búsqueda usa `:buscar`, `:proveedor` y `:tipo` como parámetros. Hibernate genera el SQL y enlaza los valores; la entrada del usuario no se concatena en la consulta. Spring Data interpreta nombres como `findFirstByOrderByPrecioDescIdProductoAsc`; Hibernate no interpreta por sí solo el nombre de ese método.

## 9. Recorrido completo de una consulta y su respuesta

Ejemplo: **GET `/api/servicios/producto-mas-caro`**.

```mermaid
sequenceDiagram
    participant C as Cliente
    participant W as Spring MVC y controlador
    participant S as Servicio
    participant R as Repositorio JPA
    participant H as Hibernate
    participant D as JDBC y driver PostgreSQL
    participant P as PostgreSQL
    C->>W: HTTP GET /api/servicios/producto-mas-caro
    W->>S: masCaro()
    S->>R: findFirstByOrderByPrecioDescIdProductoAsc()
    R->>H: Consulta de la entidad
    H->>D: SQL con parametros
    D->>P: Ejecutar consulta
    P-->>D: Filas del resultado
    D-->>H: Resultados JDBC
    H-->>R: Entidad Producto, si existe
    R-->>S: Optional de Producto
    S-->>W: Producto o excepcion
    Note over W: Construye DTO; Jackson lo serializa
    W-->>C: Respuesta HTTP con JSON y estado
```

1. **El cliente envía HTTP.** Puede ser JavaScript en la interfaz o un cliente como PowerShell. La petición contiene método, ruta y encabezados; este GET no necesita un cuerpo JSON.
2. **Spring MVC identifica el endpoint.** Relaciona el método HTTP y la ruta con las anotaciones del controlador. El filtro de la aplicación aplica límites y cabeceras; Spring también procesa y valida los parámetros necesarios.
3. **El controlador llama al servicio.** El método `masCaro()` del controlador llama a `servicio.masCaro()` mediante una referencia Java ya inyectada.
4. **El servicio llama al repositorio.** Ejecuta `productos.findFirstByOrderByPrecioDescIdProductoAsc()`. El criterio es mayor precio y, en caso de empate, menor identificador.
5. **Spring Data JPA y Hibernate preparan la consulta.** Spring Data interpreta el método y usa JPA; Hibernate genera el SQL necesario y solicita una conexión del DataSource. El repositorio carga también las relaciones de tipo, proveedor y unidad mediante `@EntityGraph`.
6. **Se consulta PostgreSQL.** Hibernate utiliza JDBC y el controlador PostgreSQL. PostgreSQL ejecuta la instrucción y devuelve sus resultados.
7. **Hibernate realiza el mapeo.** Para esta consulta, construye la entidad `Producto` y sus relaciones a partir de las filas. El repositorio devuelve `Optional<Producto>`: un resultado con producto o vacío.
8. **El servicio interpreta el resultado.** Si existe, devuelve el producto. Si no hay productos, lanza una excepción. No sabe si existe antes de consultar: primero recibe el resultado y después decide.
9. **El controlador prepara el DTO.** Para el resultado correcto ejecuta `ProductoResponse.desde(servicio.masCaro())`. Eso crea un objeto Java de respuesta a partir de la entidad; no modifica el producto original.
10. **Spring MVC utiliza Jackson.** Serializa el DTO a texto JSON y envía la respuesta HTTP. El caso correcto devuelve 200. Si no había producto, `ApiExceptionHandler` prepara un DTO de error y una respuesta 404, también en JSON.
11. **El cliente interpreta la respuesta.** La interfaz lee el JSON y utiliza sus datos para actualizar la pantalla. La respuesta vuelve al cliente que realizó la petición; la base no envía JSON directamente al navegador.

El backend completo incluye controlador, servicio, repositorios y configuración. Las entidades o los DTO que pasan entre esas capas siguen siendo objetos Java; no se convierten a JSON entre cada llamada.

### Entidad, DTO y JSON

| Elemento | Qué representa | Ejemplo en Cogas |
| --- | --- | --- |
| Entidad | Objeto Java mapeado a la base mediante JPA | `Producto`, con relaciones a `TipoProducto`, `Proveedor` y `UnidadMedida` |
| DTO de entrada | Objeto Java con los campos que recibe una operación | `ProductoRequest` al registrar o editar |
| DTO de salida | Objeto Java con los campos que devuelve la API | `ProductoResponse`, con descripción, precio, IDs y datos de los catálogos |
| JSON | Texto que representa los datos para enviarlos por HTTP | Un objeto con claves como `descripcion` y `precio` |

Un DTO puede tener menos campos, los mismos o combinar datos de varias entidades. Su finalidad es definir el contrato de comunicación; no es una tabla, un archivo ni un mecanismo de almacenamiento temporal. En este proyecto se representa con objetos Java que normalmente viven en memoria durante la operación.

**Primero se construye el DTO y después se serializa a JSON.** No se convierte un objeto «a JSON dentro del DTO». Spring Data JPA se ocupa de los repositorios; la integración de Spring MVC con Jackson se ocupa del JSON HTTP.

**Serializar** significa representar un objeto en texto o bytes; aquí, convertir el DTO a texto JSON. **Deserializar** es interpretar ese JSON y construir el objeto correspondiente. `SERIAL` y las columnas de identidad de PostgreSQL tienen otra función: generar identificadores. En nuestras tablas usamos identidad, mapeada con `GenerationType.IDENTITY`.

## 10. CRUD de productos

CRUD significa **Create, Read, Update, Delete**: crear, consultar, actualizar y eliminar.

| Operación | Método y ruta | Entrada | Trabajo del servicio y repositorio | Respuesta correcta |
| --- | --- | --- | --- | --- |
| Crear | POST `/api/productos` | JSON convertido a `ProductoRequest` | Validar, crear entidad y ejecutar `saveAndFlush` | 201, encabezado `Location` y `OperacionResponse` con ID y mensaje |
| Consultar lista | GET `/api/productos` | Parámetros de búsqueda, filtros y paginación | Validar límites y consultar con `PageRequest` | 200 y `PaginaProductosResponse` |
| Consultar uno | GET `/api/productos/{id}` | Identificador en la ruta | Buscar mediante `findById` y tratar el resultado vacío | 200 y `ProductoResponse` |
| Actualizar | PUT `/api/productos/{id}` | ID y JSON con todos los campos de `ProductoRequest` | Buscar el producto, validar, actualizar sus atributos y ejecutar `saveAndFlush` | 200 y `OperacionResponse` |
| Eliminar | DELETE `/api/productos/{id}` | Identificador en la ruta | Buscar el producto, ejecutar `delete` y `flush` | 200 y `OperacionResponse` con el mensaje |

En proveedores, tipos y unidades tenemos consulta y selección, sin CRUD de administración desde la API actual. Las tres consultas del curso son:

- GET `/api/servicios/producto-mas-caro`.
- GET `/api/servicios/por-proveedor?id=1`, con paginación.
- GET `/api/servicios/por-tipo?id=5`, con paginación.

### Qué ocurre al recibir un POST con JSON

1. El cliente envía POST `/api/productos`, `Content-Type: application/json` y el JSON en el cuerpo.
2. Spring MVC utiliza Jackson para deserializar ese cuerpo en un `ProductoRequest`, gracias a `@RequestBody`.
3. `@Valid` activa la validación de las anotaciones del DTO. Los datos inválidos producen una respuesta de error antes de ejecutar el método de registro.
4. El controlador llama a `servicio.guardar(null, datos)`. El `null` indica una creación; en una edición se pasa el identificador existente.
5. El servicio normaliza y valida los valores, convierte el precio a `BigDecimal`, obtiene referencias a los catálogos y construye una entidad `Producto`.
6. El repositorio ejecuta `saveAndFlush`. Hibernate realiza el INSERT utilizando JDBC; PostgreSQL comprueba las restricciones y genera el identificador.
7. Spring confirma la transacción cuando la llamada transaccional al servicio termina correctamente. Si falla, no se devuelve una confirmación de registro exitoso.
8. El controlador devuelve un `OperacionResponse` con el identificador y el mensaje. Spring MVC lo serializa con Jackson y envía HTTP 201.

El cuerpo de entrada tiene este formato:

```json
{
  "descripcion": "Producto temporal de la guia",
  "precio": "4.80",
  "idTipo": "1",
  "idProveedor": "1",
  "idUnidad": "1"
}
```

Los valores del DTO de entrada son cadenas para validar su formato. El servicio convierte los IDs a enteros y el precio a `BigDecimal`. Las respuestas de producto tienen IDs numéricos y precio decimal. Los IDs de catálogo deben existir en la base seleccionada.

Ejemplo de respuesta de registro; el identificador es ilustrativo:

```http
HTTP/1.1 201 Created
Content-Type: application/json
Location: /api/productos/123

{"idProducto":123,"mensaje":"Producto registrado correctamente."}
```

### Transacciones, flush y restricciones

`@Transactional` delimita una operación de base de datos. Las consultas del servicio usan transacciones de lectura; crear, editar y eliminar utilizan transacciones de escritura.

`flush` envía a la base los cambios pendientes del contexto de persistencia; **no equivale a confirmar la transacción**. La confirmación, o commit, ocurre al completar correctamente la llamada transaccional. Ante errores como una referencia inexistente se revierte la operación y se devuelve una respuesta de error.

Las validaciones se complementan: el DTO verifica campos y formatos, el servicio aplica reglas y PostgreSQL hace cumplir las restricciones de las tablas. Las anotaciones de mapeo JPA no sustituyen la validación del JSON recibido.

`spring.jpa.hibernate.ddl-auto=validate` comprueba la compatibilidad de las entidades con las tablas existentes. No crea ni actualiza automáticamente el esquema. Los scripts de `database/` preparan la base durante la instalación.

### Probar todo el CRUD sin interfaz ni Postman

Inicia Cogas con el modo deseado según la sección 4. En otra terminal de PowerShell ejecuta el siguiente bloque. **Crea un producto temporal real, lo consulta, lo edita y lo elimina al terminar.** Si ya se recibió el identificador, el bloque `finally` intenta eliminarlo aunque falle un paso posterior.

```powershell
$ErrorActionPreference = 'Stop'
$baseUrl = 'http://localhost:8080/api/productos'
$datos = @{
    descripcion = 'Producto temporal de la guia'
    precio = '4.80'
    idTipo = '1'
    idProveedor = '1'
    idUnidad = '1'
}
$creado = $null

try {
    # CREATE: POST con JSON en el cuerpo.
    $creado = Invoke-RestMethod -Uri $baseUrl -Method POST `
        -ContentType 'application/json' -Body ($datos | ConvertTo-Json)
    $creado
    $urlProducto = "$baseUrl/$($creado.idProducto)"

    # READ: GET a la URL del producto creado.
    Invoke-RestMethod -Uri $urlProducto -Method GET

    # UPDATE: PUT con todos los campos y el nuevo precio.
    $datos.descripcion = 'Producto temporal actualizado'
    $datos.precio = '5.25'
    Invoke-RestMethod -Uri $urlProducto -Method PUT `
        -ContentType 'application/json' -Body ($datos | ConvertTo-Json)
    Invoke-RestMethod -Uri $urlProducto -Method GET
}
finally {
    # DELETE: eliminar solo el producto creado por este ejemplo.
    if ($null -ne $creado -and $null -ne $creado.idProducto) {
        Invoke-RestMethod -Uri "$baseUrl/$($creado.idProducto)" -Method DELETE
    }
}
```

La URL indica el destino. El método HTTP indica la operación y el cuerpo contiene el JSON. **Escribir una URL en la barra del navegador envía GET**; para insertar debes enviar POST, como hace el comando anterior. GET no inserta productos aunque se añadan campos de registro a los parámetros de la URL.

### Respuestas de error

`ApiExceptionHandler`, anotado con `@RestControllerAdvice`, transforma los errores en estados HTTP y DTO de error. Jackson también serializa esas respuestas.

| Estado | Significado en esta API |
| --- | --- |
| 400 | Datos, parámetros o JSON inválidos |
| 404 | Producto o recurso no encontrado |
| 405 | Método HTTP no permitido para la ruta |
| 409 | Restricción de integridad, como una referencia de catálogo inexistente |
| 413 | Solicitud que supera el límite de tamaño permitido |
| 415 | Formato de entrada no admitido; POST/PUT requieren JSON |
| 503 | Fallo de acceso a la base de datos |
| 500 | Error interno no contemplado por los casos anteriores |

El cuerpo contiene `error` y, cuando corresponde, `campos` con los mensajes de validación. El cliente debe comprobar el estado de la respuesta y mostrar el resultado o el error; no debe interpretar cualquier respuesta HTTP como un registro exitoso.

## 11. Cómo JavaScript utiliza el JSON

La interfaz usa `fetch` para enviar peticiones. En una creación o edición, `JSON.stringify` convierte los datos del formulario en texto JSON para el cuerpo de la solicitud. En el backend, Jackson los convierte al DTO de entrada.

Ejemplo de lectura de una respuesta, simplificado para mostrar los pasos:

```javascript
const respuesta = await fetch('/api/servicios/producto-mas-caro');
const datos = await respuesta.json();

if (!respuesta.ok) {
  throw new Error(datos.error || 'No se pudo completar la consulta.');
}

console.log(datos.descripcion, datos.precio);
```

`fetch` obtiene una respuesta HTTP. `await respuesta.json()` lee su cuerpo e interpreta el texto JSON para obtener un valor JavaScript; no hace otra consulta a la base. `await` espera a que termine esa operación antes de continuar.

Con los datos recibidos, `view/app.js` actualiza elementos del DOM, es decir, la estructura de la página HTML. HTML define los elementos, JavaScript actualiza su contenido y CSS determina su apariencia. La interfaz muestra confirmaciones y errores; el navegador no recibe conexiones ni credenciales de PostgreSQL.

En las herramientas del navegador, **Network → petición → Response** permite inspeccionar el texto de respuesta; **Preview** puede mostrarlo como estructura. En un POST/PUT, la sección de carga útil de la petición permite revisar el JSON enviado.

## 12. Referencias para estudiar

- [Jakarta Persistence: especificación y API de JPA](https://jakarta.ee/specifications/persistence/).
- [Spring Data JPA: repositorios](https://docs.spring.io/spring-data/jpa/reference/).
- [Spring Data JPA: consultas y parámetros](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html).
- [Hibernate ORM: implementación y mapeo objeto-relacional](https://hibernate.org/orm/).
- [JDBC en Java 25](https://docs.oracle.com/en/java/javase/25/docs/api/java.sql/java/sql/package-summary.html).
- [Spring MVC: procesamiento de peticiones con DispatcherServlet](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet.html).
- [Jackson: proyecto de procesamiento de JSON](https://github.com/FasterXML/jackson).
- [Spring Boot: integración de JSON](https://docs.spring.io/spring-boot/reference/features/json.html).

Los ejemplos de esta guía describen el código de Cogas. Los detalles de rutas y paginación se amplían en [docs/api.md](docs/api.md); la configuración y los límites, en [docs/arquitectura.md](docs/arquitectura.md).
