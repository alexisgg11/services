# Render con PostgreSQL externo

1. Crea PostgreSQL en Supabase u otro proveedor y ejecuta `database/01_tablas_y_datos.sql` en esa base.
2. En Render, crea un Blueprint desde este repositorio usando `render.yaml`.
3. Configura `DB_URL` JDBC, `DB_USER` y `DB_PASSWORD` como variables privadas. Para Supabase usa los valores de Connect → Session pooler; usuario y contraseña separados de la URL. Añade `sslmode=require` a la URL JDBC.
4. `APP_HOST=0.0.0.0` está declarado en el Blueprint. Render proporciona `PORT`.
5. Revisa `/api/health` y la interfaz en la URL asignada.

Docker compila y ejecuta con Java 25. No incluye `.env`, target ni configuración local. No se crean tablas durante el despliegue. La imagen Docker no se ha construido localmente en esta revisión porque Docker no está instalado.

La aplicación sigue siendo una demostración sin autenticación. El despliegue público no se ha realizado.

Documentación: [Docker en Render](https://render.com/docs/docker), [conexión PostgreSQL de Supabase](https://supabase.com/docs/guides/database/connecting-to-postgres).
