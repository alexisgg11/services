# Cogas: backend local con PostgreSQL en Supabase

El navegador y Java siguen en tu equipo. Solo PostgreSQL está en Supabase. No se necesita Render para esta modalidad.

## 1. Crear la base

1. Crea un proyecto en una organización Free de Supabase y establece tú mismo su contraseña de base de datos. No la envíes por el chat ni la guardes en Git.
2. Conserva PostgreSQL local y su archivo `abastecimiento/.env`.
3. Para usar los datos iniciales, ejecuta `database/01_tablas_y_datos.sql` en SQL Editor. No ejecutes `00_crear_base.sql`: Supabase ya tiene una base llamada `postgres`.
4. Para trasladar cambios existentes, exporta y restaura el esquema `panaderia` con sus datos y secuencias, omitiendo propietarios y permisos locales. Usa ese respaldo en lugar de cargar los datos iniciales. La base local se conserva.

## 2. Configurar una conexión separada

Desde la raíz del repositorio, copia la plantilla solo si el archivo privado todavía no existe:

```powershell
Copy-Item abastecimiento/.env.supabase.example abastecimiento/.env.supabase
```

En Supabase abre **Connect → Session pooler**. Copia el host y usuario exactos y configura el archivo privado:

```dotenv
DB_URL=jdbc:postgresql://HOST_SESSION_POOLER:5432/postgres?sslmode=require
DB_USER=postgres.REFERENCIA_PROYECTO
DB_PASSWORD=TU_CONTRASEÑA_DE_LA_BASE
APP_HOST=127.0.0.1
PORT=8080
DB_POOL_SIZE=5
HTTP_THREADS=8
DB_QUERY_TIMEOUT_SECONDS=10
```

Sustituye los marcadores por los valores reales. Para la conexión JDBC no se necesitan claves `anon` ni `service_role`. El pooler de sesión admite IPv4; la conexión directa gratuita usa IPv6. [Documentación oficial](https://supabase.com/docs/guides/database/connecting-to-postgres)

Git ignora `.env` y `.env.supabase`, pero publica sus plantillas sin credenciales. Las variables del sistema tienen prioridad sobre los archivos: elimina los valores globales de conexión si están sobrescribiendo la base que seleccionaste.

## 3. Iniciar y comprobar

Detén el servidor que ya esté usando el puerto antes de cambiar de base. Después ejecuta:

```powershell
# Java local conectado a Supabase:
.\scripts\run.ps1 -BaseDatos Supabase

# Volver a Java y PostgreSQL locales:
.\scripts\run.ps1
```

Ambos modos entregan la interfaz en `http://127.0.0.1:8080`, salvo que configures otro puerto. El modo Supabase necesita Internet y un proyecto activo; no requiere que PostgreSQL local esté encendido.

Comprueba `/api/health`, el catálogo, la paginación y las tres consultas. Los datos no se sincronizan entre las bases. Si falta el archivo seleccionado, el script se detiene; no cambia silenciosamente a la otra base.

El plan Free incluye 500 MB por proyecto y puede pausarse tras una semana sin actividad. No incluye copias automáticas. [Condiciones de Supabase](https://supabase.com/pricing)

La preparación del modo de conexión no confirma una migración realizada: la conexión y los datos deben comprobarse contra el proyecto real.
