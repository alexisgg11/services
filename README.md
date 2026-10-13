# Cogas · Abastecimiento de panadería

Aplicación web con Java 25, Spring Boot 4.1.1, API REST y PostgreSQL. Permite registrar, editar, eliminar y buscar productos, consultar proveedores, tipos y unidades, y ejecutar tres servicios: producto más caro, productos por proveedor y productos por tipo.

La base inicial contiene cuatro tablas y 50 productos. Los proveedores, tipos y unidades se consultan y seleccionan; no tienen formularios de registro o edición.

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

Actualmente hay **29 pruebas** de configuración, validaciones, servicios, DTO y controladores REST con Spring MVC. El resultado esperado es `BUILD SUCCESS`, sin errores ni fallos.

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
      model/                  Modelos internos del dominio
      dto/                    Solicitudes y respuestas de la API REST
      controller/             @RestController y errores centralizados
      service/                @Service, validaciones y reglas de negocio
      repository/             @Repository y SQL JDBC parametrizado
      util/                   Diagnósticos de arranque sin credenciales
    src/main/resources/view/  Interfaz HTML, CSS y JavaScript
    src/test/java/com/alexis/  Pruebas del backend
  database/                   Creación de tablas y datos iniciales
  scripts/                    Arranque y pruebas de API
  docs/                       Arquitectura, API y despliegue
  Dockerfile                  Empaquetado para despliegue
  render.yaml                 Configuración de Render
```

- [Arquitectura](docs/arquitectura.md)
- [API y servicios](docs/api.md)
- [Backend local con Supabase](docs/supabase-local.md)
- [Despliegue en Render](docs/despliegue.md)

Cogas es un monolito por capas con Spring Boot que expone una API REST. Spring crea e inyecta los componentes; Jackson convierte los DTO a JSON. El alcance actual no incluye autenticación, compras ni movimientos de stock; **Admin** es una etiqueta del menú.
