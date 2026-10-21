-- Ejecutar una sola vez sobre la base existente, local o Supabase.
-- Puede repetirse: conserva los productos y sus estados actuales.
BEGIN;
ALTER TABLE panaderia.producto
 ADD COLUMN IF NOT EXISTS estado BOOLEAN NOT NULL DEFAULT TRUE;
COMMENT ON COLUMN panaderia.producto.estado IS
 'true: producto activo; false: eliminado logicamente, conservado en la base';
COMMIT;
