CREATE TABLE IF NOT EXISTS contactos (
    id_contactos SERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    apellidos VARCHAR(150) NOT NULL,
    celular VARCHAR(9) NOT NULL,
    distrito VARCHAR(50) NOT NULL
);
