# Comparación con la base del docente

Revisión del 18 de octubre de 2026. Se compara el código, no las afirmaciones del README original cuando difieren de la implementación.

## Copia utilizada

- Repositorio: [bmiguelbc16/SOAS-Servicio](https://github.com/bmiguelbc16/SOAS-Servicio).
- Primer commit y único commit raíz: [`6b0c54a7712fc2efedbf12333b334e0faaf5f1f5`](https://github.com/bmiguelbc16/SOAS-Servicio/tree/6b0c54a7712fc2efedbf12333b334e0faaf5f1f5).
- Fecha: 14 de octubre de 2026, 22:39, UTC−05:00.
- Mensaje: «Baseline: versión del ingeniero tal cual se desarrolló en clases (Spring Boot 4.1.1 + PostgreSQL)».
- Destino: `services_docente/`, dentro de este repositorio. Sus 15 archivos se verificaron contra los hashes de los blobs originales. No se añadió un repositorio Git dentro de esa carpeta ni se modificó el código copiado.
- Las mejoras posteriores de Maven, selección automática de puertos e inserción GET con JSON en la URL quedan fuera de esta copia.

## Base y adaptación de Cogas

| Aspecto | Primer commit del docente | Cogas |
| --- | --- | --- |
| Java | 17 en `pom.xml` | 25, requisito del proyecto |
| Spring Boot | 4.1.1 | 4.1.1 |
| Arranque | `@SpringBootApplication` y `SpringApplication.run` | Mismo patrón en `App` |
| Capas | controller, service, repository, model, dtos y config | Mismas responsabilidades; carpeta `dto` en singular |
| Dependencias | Web MVC, Data JPA, PostgreSQL | Las mismas bases, más validación de solicitudes, configuración privada y pruebas |
| Modelos | `Contacto` con `@Entity` y `@Table` | Cuatro entidades JPA para producto, tipo, proveedor y unidad |
| Persistencia | Interfaz `JpaRepository<Contacto, Integer>` | Interfaces `JpaRepository` para las cuatro entidades |
| Búsquedas | Método derivado de Spring Data | JPQL con parámetros y método derivado para el producto más caro |
| Inyección | Por constructor | Por constructor |
| JSON de registro | DTO con `@RequestBody` en POST | `ProductoRequest` con `@RequestBody` y `@Valid`; respuestas DTO |
| Base de datos | PostgreSQL local | PostgreSQL local o Supabase, con archivos privados separados |
| Tablas y datos al arrancar | Ejecuta `schema.sql`; inserta cinco ejemplos si la tabla está vacía | Solo valida las cuatro tablas existentes; los scripts SQL se ejecutan manualmente |
| Interfaz | HTML servido por Spring | HTML, CSS y JavaScript servidos por Spring en `resources/view` |
| Alcance | Listado, búsqueda y registro de contactos | Catálogo de abastecimiento, CRUD de productos y tres servicios de consulta |

La diferencia principal era la persistencia manual con JDBC. Por indicación del usuario se sustituyó por Spring Data JPA/Hibernate. Las consultas de negocio ya no abren conexiones ni recorren `ResultSet`; Hibernate realiza ese trabajo. Se conservó `ConexionDB` como configuración del pool y comprobación de conectividad, que JPA utiliza como `DataSource`.

Los modelos de resultados `PaginaProductos` y `Resumen` no son tablas ni llevan `@Entity`. Los DTO no se usan como entidades. Los catálogos siguen siendo de consulta: no se añadieron formularios ni servicios de administración adicionales.

## La inserción por URL

En la base inicial existe `POST /api/contactos/insertar/`, que requiere `@RequestBody ContactosDTO`. No existe un GET de inserción. Por tanto, escribir esa ruta en la barra del navegador no ejecuta su método POST. Es una conclusión de la ruta y el método declarados, no una reproducción del fallo que ocurrió en clase.

La inserción GET con `?contacto={...}` apareció en el commit posterior [`93ed7a7`](https://github.com/bmiguelbc16/SOAS-Servicio/commit/93ed7a7698719f936cafd48952909f3de575caa5). No pertenece a la primera subida y no se incorpora a Cogas. En Cogas se usa POST `/api/productos`, `Content-Type: application/json` y JSON en el cuerpo. Se puede probar con PowerShell, sin interfaz ni Postman; el ejemplo está en [api.md](api.md).

Otros detalles que explican diferencias con la referencia:

- El DTO del docente llama `apellido` al campo de entrada, mientras la entidad usa `apellidos`. En una prueba de su POST hay que respetar `apellido` y la ruta exacta con `/` final.
- La búsqueda de su servicio construye `new ContactosDTO(contacto.getNombre())`; únicamente rellena el nombre, aunque la página espera también apellido, celular y distrito. Cogas devuelve los campos documentados en sus DTO.
- El README original afirma que no hay inserción y muestra una respuesta de búsqueda más completa que la implementada. Se conserva tal cual para no alterar la primera versión.
- `.mvn/maven.config` original contiene una ruta absoluta de otro equipo. Se conserva como referencia; Cogas no usa esa configuración ni depende de ella.

## Orden y alcance

`abastecimiento/` es la única aplicación de Cogas. `services_docente/` es una referencia separada, excluida del contexto Docker. Las pruebas y los comandos de arranque apuntan explícitamente a `abastecimiento/pom.xml`.

El flujo es: HTTP → controlador y DTO de entrada → servicio y transacción → repositorio JPA → PostgreSQL. La respuesta vuelve como entidad o resultado de consulta, se transforma a DTO y Jackson genera JSON. Las relaciones de producto se cargan al consultar; `open-in-view` está desactivado.

Se mantienen validaciones, paginación en servidor, consultas parametrizadas, confirmaciones y manejo de errores. No se añadieron microservicios, autenticación, sincronización de bases ni generación automática de datos.

## Comprobación

La verificación incluye compilación con Java 25, pruebas de configuración, servicio y controlador, y pruebas HTTP contra las dos bases. El script `scripts/test-api.ps1` registra, edita y elimina únicamente un producto temporal; comprueba que el total original se conserva. Una prueba de controlador comprueba que parámetros de registro en una petición GET no llaman al método de guardado.

Resultados: 31 pruebas de backend aprobadas; API verificada con PostgreSQL local y Supabase, conservando respectivamente 50 y 51 productos. También se comprobó el comando `scripts/run.ps1 -BaseDatos Supabase`, el catálogo en el navegador y la ausencia de errores en su consola.

El código de referencia se conserva para lectura y comparación. No se conecta a las bases de Cogas ni se ejecuta su inicializador de contactos.
