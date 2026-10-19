# Servicio de contactos

Aplicación web para consultar contactos almacenados en PostgreSQL. Incluye una página de búsqueda y una API REST que devuelve los datos en JSON. Permite buscar por nombre o apellido y consultar el celular y el distrito de cada persona.

La API ofrece operaciones de lectura. No incluye formularios ni endpoints para crear, editar o eliminar contactos.

## Tecnologías

- Java 17 (versión definida en `pom.xml`).
- Spring Boot 4.1.1 y Spring Web MVC.
- Spring Data JPA / Hibernate para consultar la base de datos.
- PostgreSQL y su controlador JDBC.
- Maven para descargar dependencias, compilar y ejecutar.
- Interfaz HTML disponible desde el mismo servidor.

## Requisitos

1. JDK 17 o posterior. Se recomienda Java 17 para reproducir la configuración verificada.
2. Maven 3.6.3 o posterior disponible en el `PATH`.
3. PostgreSQL instalado y en ejecución, con un usuario y su contraseña.
4. Conexión a Internet durante la primera compilación para descargar dependencias.
5. Puerto 8080 disponible, o configurar otro mediante `SERVER_PORT`.

Verifica Java y Maven en PowerShell:

```powershell
java -version
mvn -version
```

La salida de `mvn -version` muestra el JDK que Maven utiliza. Si difiere del esperado, revisa `JAVA_HOME` y `PATH`.

## 1. Abrir el proyecto

Ejecuta los siguientes comandos desde PowerShell:

```powershell
Set-Location 'C:\Users\USUARIO\Documents\Proyectos\Servicios'
```

Todos los comandos siguientes se ejecutan desde esta carpeta, donde están `pom.xml` y `.env`.

## 2. Preparar PostgreSQL

Inicia el servicio de PostgreSQL. En pgAdmin, conectado al servidor local, abre Query Tool sobre una base existente como `postgres` y ejecuta una sola vez:

```sql
CREATE DATABASE contactos_servicios;
```

Si la base ya existe, conserva la existente y omite este paso. La aplicación necesita que la base esté creada antes de arrancar.

Al iniciar, `src/main/resources/schema.sql` crea la tabla `contactos` si todavía no existe. Hibernate valida su estructura. Si ya tienes una tabla con ese nombre, debe ser compatible con estas columnas:

| Columna | Tipo | Descripción |
| --- | --- | --- |
| `id_contactos` | `SERIAL PRIMARY KEY` | Identificador del contacto |
| `nombre` | `VARCHAR(150) NOT NULL` | Nombre |
| `apellidos` | `VARCHAR(150) NOT NULL` | Apellidos |
| `celular` | `VARCHAR(9) NOT NULL` | Celular guardado como texto |
| `distrito` | `VARCHAR(50) NOT NULL` | Distrito |

Cuando la tabla está vacía, la aplicación inserta cinco contactos ficticios. Si ya contiene registros, los conserva y no agrega los ejemplos.

## 3. Configurar la conexión

Si no existe `.env`, copia la plantilla sin sobrescribir una configuración existente:

```powershell
if (-not (Test-Path '.env')) {
    Copy-Item '.env.example' '.env'
}
notepad .env
```

Completa los valores con tu configuración local:

```properties
DB_URL=jdbc:postgresql://localhost:5432/contactos_servicios
DB_USERNAME=postgres
DB_PASSWORD=tu_clave_de_PostgreSQL
SERVER_PORT=8080
```

- `DB_URL`: servidor, puerto y nombre de la base de datos.
- `DB_USERNAME`: usuario con permisos para conectar, crear la tabla y consultar e insertar datos.
- `DB_PASSWORD`: contraseña de ese usuario. Es obligatoria.
- `SERVER_PORT`: puerto HTTP; es opcional y su valor predeterminado es `8080`.

El archivo `.env` se lee como un archivo de propiedades de Spring desde el directorio de ejecución. No uses la sintaxis `export`. Está excluido de Git; no publiques la contraseña. También puedes definir estas opciones como variables de entorno.

## 4. Descargar dependencias y compilar

```powershell
mvn clean package
```

Maven descarga automáticamente las dependencias declaradas en `pom.xml`. No es necesario instalar por separado las bibliotecas de Spring ni el controlador JDBC.

Espera el mensaje `BUILD SUCCESS`. El archivo ejecutable se genera en:

```text
target/contactos-servicios-0.0.1-SNAPSHOT.jar
```

Actualmente no hay pruebas automatizadas en el proyecto; una compilación correcta no verifica por sí sola la conexión a PostgreSQL.

### Caché de Maven de este equipo

El archivo `.mvn/maven.config` configura esta ubicación:

```text
-Dmaven.repo.local=C:/Users/USUARIO/.m2/repository
```

Esto evita el error de permisos observado cuando Maven intentaba escribir en `C:\.m2\repository`. Si copias el proyecto a otro equipo o usuario, actualiza esa ruta a una carpeta escribible de ese usuario. Si Maven ya tiene una ubicación predeterminada correcta, puedes retirar esta opción.

## 5. Ejecutar el proyecto

Con PostgreSQL en ejecución y `.env` configurado:

```powershell
mvn spring-boot:run
```

Espera los mensajes `Tomcat started on port 8080` y `Started ContactosApplication`. Mantén abierta esa terminal mientras usas la aplicación.

Abre en el navegador:

```text
http://localhost:8080/
```

Escribe un nombre o apellido en la página para buscar contactos. La búsqueda admite coincidencias parciales y no distingue mayúsculas de minúsculas.

Para detener el servidor, presiona **Ctrl+C** en la terminal donde lo ejecutaste. Detén esa instancia antes de iniciar otra en el mismo puerto.

### Ejecutar el JAR compilado

Como alternativa, desde la raíz del proyecto:

```powershell
java -jar .\target\contactos-servicios-0.0.1-SNAPSHOT.jar
```

Usa una sola de las dos formas de ejecución a la vez. Mantener el directorio de trabajo en la raíz permite que la aplicación encuentre `.env`.

### Usar otro puerto

Puedes cambiar `SERVER_PORT` en `.env` o definirlo para la sesión de PowerShell:

```powershell
$env:SERVER_PORT = '8081'
mvn spring-boot:run
```

En ese caso, abre `http://localhost:8081/`. Para retirar la variable de la sesión después de detener el servidor:

```powershell
Remove-Item Env:SERVER_PORT
```

## API REST y comprobación

| Método | Ruta | Resultado |
| --- | --- | --- |
| `GET` | `/` | Interfaz web de búsqueda |
| `GET` | `/api/contactos` | Lista de todos los contactos |
| `GET` | `/api/contactos/buscar?persona=Diego` | Contactos cuyo nombre o apellido contiene el término |

Ejemplos en PowerShell, con el servidor iniciado:

```powershell
Invoke-RestMethod 'http://localhost:8080/api/contactos'
Invoke-RestMethod 'http://localhost:8080/api/contactos/buscar?persona=Diego'
```

Ejemplo de respuesta de búsqueda con los datos iniciales:

```json
[
  {
    "idContactos": 3,
    "nombre": "Diego",
    "apellidos": "Salazar",
    "celular": "900000003",
    "distrito": "Talara"
  }
]
```

El identificador puede variar según los registros y la secuencia de la base de datos. Cuando no hay coincidencias, la búsqueda devuelve `[]`. Un término vacío o compuesto por espacios también devuelve `[]`. El parámetro `persona` es obligatorio: omitirlo produce una respuesta HTTP 400.

## Estructura del proyecto

```text
Servicios/
├── .mvn/maven.config                  # Ubicación de la caché de Maven
├── .env.example                       # Plantilla de conexión
├── .env                               # Configuración local excluida de Git
├── pom.xml                            # Dependencias y compilación
├── src/main/java/com/servicios/contactos/
│   ├── ContactosApplication.java      # Punto de entrada
│   ├── config/                        # Carga inicial de contactos ficticios
│   ├── controller/                    # Endpoints REST
│   ├── model/                         # Entidad Contacto
│   ├── repository/                    # Consultas con Spring Data JPA
│   └── service/                       # Lógica de consulta y búsqueda
├── src/main/resources/
│   ├── application.properties         # Configuración de Spring
│   ├── schema.sql                     # Creación de la tabla
│   └── static/index.html              # Interfaz web
└── target/                            # Resultado de compilación
```

## Solución de problemas

### `BUILD FAILURE` / `Process terminated with exit code: 1`

Este mensaje final no indica por sí solo la causa. Lee las líneas anteriores y busca `APPLICATION FAILED TO START`, `Caused by` o la descripción concreta del error. También puedes ejecutar:

```powershell
mvn -e spring-boot:run
```

### Puerto 8080 ocupado

Si aparece `Port 8080 was already in use`, ya hay un proceso usando ese puerto. Detén la instancia anterior con Ctrl+C en su terminal o cambia `SERVER_PORT`.

Para identificar el PID que escucha en 8080:

```powershell
netstat -ano | Select-String ':8080'
```

Revisa la fila `LISTENING` y comprueba qué programa corresponde al PID antes de detenerlo:

```powershell
Get-Process -Id <PID>
```

Sustituye `<PID>` por el número real. Si confirmas que es una instancia anterior de este proyecto y no puedes acceder a su terminal, puedes detenerla con `Stop-Process -Id <PID>`.

### Error de conexión a PostgreSQL

- `Connection refused`: verifica que PostgreSQL esté iniciado y que el host y puerto de `DB_URL` sean correctos.
- `database ... does not exist`: crea la base indicada en `DB_URL`.
- `password authentication failed`: revisa `DB_USERNAME` y `DB_PASSWORD` en `.env`.
- Error por `DB_PASSWORD` sin resolver: crea `.env`, define la contraseña y ejecuta desde la raíz del proyecto.
- Error de validación de esquema: compara la tabla existente con `schema.sql`; `CREATE TABLE IF NOT EXISTS` no modifica columnas de una tabla que ya existe.

### Maven o Java no se reconoce

Verifica la instalación, `PATH` y `JAVA_HOME`. Abre una terminal nueva después de modificar las variables y comprueba nuevamente `java -version` y `mvn -version`.

### Maven no puede crear el repositorio local

Comprueba que la ruta de `.mvn/maven.config` exista o pueda crearse y que tu usuario tenga permisos de escritura. Ajusta la ruta si utilizas otro usuario de Windows.

### Fallo al descargar dependencias

Revisa la conexión a Internet y la configuración de proxy, si tu red la requiere. Luego vuelve a ejecutar `mvn clean package`.

## Verificación realizada en el equipo

Se verificó la compilación con Maven 3.9.15 y Java 17.0.16, el arranque con PostgreSQL 18 y una consulta a `/api/contactos` con respuesta HTTP 200 y cinco contactos. La instancia utilizada para esa comprobación se detuvo para dejar libre el puerto 8080.
