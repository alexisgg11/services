# Cogas: PostgreSQL local

1. En pgAdmin, abre Query Tool en `postgres` y ejecuta `00_crear_base.sql` fuera de una transacción.
2. Actualiza Databases y abre Query Tool en `panaderia_abastecimiento`.
3. Ejecuta `01_tablas_y_datos.sql`.
4. Configura `abastecimiento/.env` y ejecuta `.\scripts\run.ps1 -BaseDatos Local` desde la carpeta `services`, siguiendo el README principal.

Comprobación:

```sql
SELECT count(*) FROM panaderia.producto WHERE estado = true; -- 50 en una instalación nueva
```

Estos archivos son la única fuente SQL de instalación. Los datos de muestra y sus RUC son ficticios. El esquema `panaderia` no modifica la tabla anterior de contactos. No vuelvas a ejecutar la carga para arrancar el servidor: puede reponer ejemplos eliminados.

## Base existente

Ejecuta `02_eliminacion_logica.sql` sobre la base local y sobre Supabase si utilizas ambos modos. Añade `estado BOOLEAN NOT NULL DEFAULT TRUE` sin borrar ni renumerar productos. La migración puede repetirse y conserva los estados existentes. La instalación nueva con `01_tablas_y_datos.sql` ya incluye la columna.

DELETE desde la API marca `estado = false`. La fila permanece en PostgreSQL y deja de aparecer en las consultas de la aplicación. Para verla:

```sql
SELECT id_producto, descripcion, estado
FROM panaderia.producto
WHERE estado = false
ORDER BY id_producto;
```
