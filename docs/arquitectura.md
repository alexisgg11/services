# Arquitectura

## Capas

`view → controller (DTO) → service → repository → PostgreSQL`. La respuesta vuelve como modelo Java, se transforma a DTO y Jackson la serializa a JSON.

- `App` usa `@SpringBootApplication` y `SpringApplication.run` para cargar Spring y arrancar la aplicación. Spring crea los componentes, inyecta sus dependencias por constructor y coordina su cierre.
- `config/SpringConfig` registra la configuración, el pool y el DataSource; configura el servidor Tomcat embebido y verifica la base al arrancar.
- `dto` contiene `ProductoRequest`, validado con Jakarta Validation, y respuestas de producto, paginación, resumen, catálogos y errores. Los modelos internos no se devuelven directamente por HTTP.
- `config/AppConfig` carga `.env` o variables del entorno y valida host, puerto, URL, usuario, contraseña y límites antes de arrancar. El entorno tiene prioridad. No usa credenciales por defecto ni imprime secretos.
- `config/ConexionDB` administra un pool HikariCP por proceso: máximo 5 conexiones por defecto, adquisición en 20 segundos configurable, sin conexiones mínimas calientes y cierre explícito. JDBC usa timeout de conexión, consulta y bloqueo.
- `repository`, registrado con `@Repository`, recibe `DataSource`, usa `PreparedStatement`, devuelve cada conexión al pool con try-with-resources y no consulta el entorno.
- `service`, registrado con `@Service`, valida los datos y no depende del servidor HTTP.
- `controller` usa `@RestController`, `@GetMapping`, `@PostMapping`, `@PutMapping` y `@DeleteMapping`. Recibe DTO JSON, llama al servicio y devuelve DTO. `ApiExceptionHandler` centraliza los errores con `@RestControllerAdvice`; `ApiFilter` aplica cabeceras, límites y rechazo de parámetros repetidos.
- Jackson, integrado por Spring Boot, serializa y deserializa JSON. `util/DiagnosticoArranque` presenta fallos sin imprimir credenciales; la vista escapa texto antes de insertar HTML.

## Configuración

Obligatorias: `APP_HOST`, `PORT`, `DB_URL`, `DB_USER`, `DB_PASSWORD`. URL, usuario y contraseña se configuran separados. Ninguno se envía al navegador. Valores locales solo figuran como ejemplo de instalación.

Opcionales: `DB_POOL_SIZE` (5, rango 1–20), `HTTP_THREADS` (8, rango 1–32), `DB_QUERY_TIMEOUT_SECONDS` (10, rango 1–60), `DB_CONNECTION_TIMEOUT_SECONDS` (20, rango 3–60).

`App.java` no contiene localhost, puertos ni credenciales. Los nombres de tablas, relaciones y restricciones pertenecen al modelo y son constantes SQL; no son valores de conexión ni se reciben del cliente.

## Datos

| Tabla | Campos |
| --- | --- |
| producto | id_producto INTEGER identity PK, descripcion VARCHAR(150), precio NUMERIC(10,2), id_tipo/id_proveedor/id_unidad INTEGER FK |
| tipo_producto | id_tipo INTEGER identity PK, descripcion VARCHAR(80) única |
| proveedor | id_proveedor INTEGER identity PK, ruc VARCHAR(11) único, nombre VARCHAR(120) |
| unidad_medida | id_unidad INTEGER identity PK, descripcion VARCHAR(50) única, simbolo VARCHAR(10) único |

Los campos son obligatorios. Precio no negativo; RUC con 11 dígitos. Java usa BigDecimal para dinero. Cada producto tiene un tipo, un proveedor y una unidad. El precio es el costo en soles por la unidad indicada; el servicio de mayor costo compara ese valor sin convertir kg a litros.

La instalación se hace manualmente con `database/`; no hay DDL ni carga de ejemplos durante el arranque. RLS bloquea acceso público de Supabase. JDBC necesita un rol propietario del esquema o permisos/políticas específicos. Para el alcance local se usa el usuario configurado por el administrador; no se generan credenciales nuevas.

## SQL y límites

Todos los valores de búsqueda, filtros, IDs, precio, tamaño y offset se enlazan con parámetros `?`. La composición de SQL solo combina fragmentos constantes internos. Los campos se seleccionan explícitamente, sin SELECT *. No se acepta SQL desde HTTP.

La paginación se ejecuta en PostgreSQL con LIMIT/OFFSET parametrizados. Conteo y página usan la misma transacción de lectura con aislamiento repeatable read. El orden por ID es estable. El resumen usa agregados SQL; la vista no descarga todos los productos para calcular indicadores.

Los catálogos están acotados a 100 elementos por clase. Si se supera ese límite, el servicio falla de forma controlada; para ampliar el alcance hay que paginarlos. Las solicitudes JSON admiten hasta 16 KiB, búsqueda hasta 150 caracteres y páginas de hasta 100 productos.

## Revisión

Se tomaron de `rosan` las prácticas de configuración privada validada, capas separadas, paginación en servidor, límites y pool. Rosan es una referencia documental y de prototipo; no se copió su stack TypeScript ni se modificaron sus archivos.

El proyecto no implementa autenticación, roles, compras ni movimientos de stock. No se presenta como un CRM completo. Para un uso operativo con datos reales, esas funciones requieren su propio alcance y pruebas.

Referencias: [pgJDBC y parámetros](https://jdbc.postgresql.org/documentation/query/), [HikariCP](https://github.com/brettwooldridge/HikariCP).

Spring Boot 4.1.1 admite Java 25. [Requisitos oficiales](https://docs.spring.io/spring-boot/system-requirements.html). El JAR ejecutable incluye las dependencias y Tomcat; se conserva JDBC con PreparedStatement, sin JPA/Hibernate.
