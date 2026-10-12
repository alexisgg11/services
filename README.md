# Miga · Abastecimiento de panadería

Java 25, PostgreSQL y una interfaz web para gestionar productos, proveedores, tipos y unidades. Incluye 50 productos de ejemplo y tres servicios de consulta.

## Instalación en Windows

**Requisitos:** JDK 25, Maven 3.9, PostgreSQL y PowerShell 7. `java` y `mvn` deben estar en PATH.

1. Clona el repositorio y entra en la carpeta:

   ```powershell
   git clone https://github.com/alexisgg11/services.git
   cd services
   ```

2. En pgAdmin, conectado a `postgres`, ejecuta [database/00_crear_base.sql](database/00_crear_base.sql). Después abre Query Tool en **panaderia_abastecimiento** y ejecuta [database/01_tablas_y_datos.sql](database/01_tablas_y_datos.sql).

3. Copia la plantilla si todavía no tienes `.env`:

   ```powershell
   Copy-Item abastecimiento/.env.example abastecimiento/.env
   ```

   Completa `abastecimiento/.env` con tu conexión. Si ya existe, edítalo sin sobrescribir tus credenciales:

   ```dotenv
   DB_URL=jdbc:postgresql://localhost:5432/panaderia_abastecimiento
   DB_USER=TU_USUARIO
   DB_PASSWORD=TU_CONTRASEÑA
   APP_HOST=127.0.0.1
   PORT=8080
   ```

4. Ejecuta desde la raíz:

   ```powershell
   .\scripts\run.ps1
   ```

5. Abre **http://127.0.0.1:8080**. Detén el servidor con `Ctrl+C`.

El script compila, ejecuta las pruebas y arranca con el JDK de `java`. Si cambias `.env`, reinicia el servidor. Si cambias `PORT`, usa ese puerto en el navegador. La aplicación no crea tablas ni carga datos al arrancar.

## Pruebas

```powershell
# Con JAVA_HOME apuntando al JDK 25:
mvn -f abastecimiento/pom.xml clean test
# Con el servidor ejecutándose; crea y elimina un producto temporal:
.\scripts\test-api.ps1 -BaseUrl http://127.0.0.1:8080
```

## Estructura

```text
services/
  abastecimiento/                 Aplicación de abastecimiento
    .env.example              Plantilla; .env real se ignora
    pom.xml
    src/main/java/com/alexis/
      App.java                Arranque y cierre
      config/                 Entorno validado y pool JDBC
      model/                  Entidades y respuestas paginadas
      repository/             SQL parametrizado
      service/                Validaciones y reglas
      controller/             API HTTP
      util/                   Serialización JSON
    src/main/resources/view/  HTML, CSS y JavaScript
    src/test/java/com/alexis/  Pruebas
  database/                   SQL de instalación
  scripts/                    Ejecución y verificación de API
  docs/                       Arquitectura, API y despliegue
  Dockerfile
  render.yaml
```

- [Arquitectura y buenas prácticas](docs/arquitectura.md)
- [Endpoints y paginación](docs/api.md)
- [Despliegue en Render](docs/despliegue.md)

La interfaz utiliza HTML/CSS/JavaScript; está inspirada en shadcn/ui. **Admin** es la etiqueta del menú, no una sesión autenticada. Este alcance es una demostración académica.
