# Render + Supabase sin perder el entorno local

Render ejecuta el servidor Java y entrega la interfaz. Supabase almacena los datos en PostgreSQL. Se conserva el mismo código, con configuraciones distintas.

## Local y nube

| Entorno | Aplicación | Base de datos | Configuración |
| --- | --- | --- | --- |
| Local | `scripts/run.ps1` | PostgreSQL de tu equipo | `abastecimiento/.env` |
| Nube | Docker en Render | PostgreSQL de Supabase | Variables privadas en Render |

No cambies el `.env` local para desplegar. Docker excluye los archivos `.env`. Las variables del entorno tienen prioridad sobre el archivo y no se envían al navegador. Los cambios de datos de una base no se replican automáticamente en la otra.

## Condiciones gratuitas

Verificadas el 12 de octubre de 2026:

- Render permite un Web Service Free con Docker. Se suspende tras 15 minutos sin tráfico y el siguiente acceso puede tardar alrededor de un minuto. Hay 750 horas gratuitas por espacio de trabajo y mes, además de límites de transferencia y compilación. [Render Free](https://render.com/docs/free)
- Supabase Free incluye PostgreSQL con 500 MB por proyecto y hasta dos proyectos activos; puede pausar proyectos tras una semana sin actividad. No incluye copias de seguridad automáticas. [Supabase Free](https://supabase.com/pricing)
- PostgreSQL Free de Render expira a los 30 días. Por eso aquí usamos Supabase para la base. [Render Free](https://render.com/docs/free)

Selecciona Free en ambas plataformas. No actives complementos de pago. Sin método de pago, Render suspende servicios cuando el exceso de uso exigiría un cobro; con método de pago puede facturar ciertos excesos. [Render FAQ](https://render.com/docs/faq)

## 1. Preparar PostgreSQL en Supabase

1. Inicia sesión en Supabase y crea un proyecto dentro de una organización Free. Elige una región cercana a la del servidor y guarda la contraseña de la base de datos fuera del repositorio.
2. Para una copia con los datos iniciales, abre SQL Editor y ejecuta `database/01_tablas_y_datos.sql`. No ejecutes `00_crear_base.sql`: Supabase ya proporciona una base llamada `postgres`.
3. Si quieres conservar los cambios hechos en la base local, exporta y restaura el esquema `panaderia` con sus datos y secuencias, omitiendo propietarios y permisos locales. Usa ese respaldo en lugar de cargar los datos de ejemplo. La restauración debe probarse en la nueva base; no borres ni reemplaces la base local.
4. En **Connect**, selecciona **Session pooler** y copia el host y usuario exactos. No deduzcas el host a partir de la región. La conexión directa gratuita usa IPv6; el pooler de sesión admite IPv4 y está indicado para un backend persistente. [Conexiones de Supabase](https://supabase.com/docs/guides/database/connecting-to-postgres)

Usamos JDBC, no la API pública de Supabase: no hacen falta claves `anon` ni `service_role`. El esquema ya activa RLS; el usuario servidor debe disponer de los permisos adecuados. No abras políticas públicas para resolver un fallo de conexión.

## 2. Desplegar en Render

1. Inicia sesión en Render. Crea un **Web Service**, conecta `alexisgg11/services` y selecciona `main`.
2. Selecciona **Docker**, deja la raíz en la raíz del repositorio y utiliza `./Dockerfile`. Selecciona el plan **Free** y configura la comprobación de salud en `/api/health`.
3. Define estas variables en Render:

   | Variable | Valor |
   | --- | --- |
   | `APP_HOST` | `0.0.0.0` |
   | `DB_URL` | `jdbc:postgresql://HOST_SESSION_POOLER:5432/postgres?sslmode=require` |
   | `DB_USER` | Usuario de Connect; normalmente `postgres.REFERENCIA_PROYECTO` |
   | `DB_PASSWORD` | Contraseña de la base de Supabase |

   Reemplaza los marcadores por los datos de Connect. Mantén usuario y contraseña separados de la URL. Render proporciona `PORT`; no uses el puerto de PostgreSQL para el servidor HTTP.
4. Inicia el despliegue. Docker compila y ejecuta con Java 25. No se crean tablas ni se cargan datos durante el arranque.

También existe `render.yaml` para crear un Blueprint. Las variables privadas se solicitan durante la instalación y no se guardan en Git.

## 3. Verificar

1. Comprueba que `/api/health` responde correctamente en la URL de Render.
2. Abre la interfaz y verifica el catálogo, la paginación y las tres consultas.
3. Comprueba las operaciones de producto con un registro temporal y elimínalo al terminar.
4. Comprueba que la instalación local sigue funcionando con su `.env` y PostgreSQL local.

El alcance actual no incluye autenticación: quien acceda a la URL pública podrá operar sobre los productos. Utiliza datos de demostración. Para restringir modificaciones hay que incorporar autenticación y autorización.

Estado de esta preparación: configuración y documentación listas; despliegue público pendiente de acceso a las cuentas y verificación. La imagen Docker no se ha construido localmente porque Docker no está instalado.

Documentación: [Docker en Render](https://render.com/docs/docker), [conexión PostgreSQL de Supabase](https://supabase.com/docs/guides/database/connecting-to-postgres).
