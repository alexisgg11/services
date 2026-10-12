package com.alexis.model;

import java.math.BigDecimal;

/** Precio de adquisición por una unidad de medida, expresado en soles. */
public record Producto(int idProducto, String descripcion, BigDecimal precio,
                       int idTipo, String tipo, int idProveedor, String proveedor,
                       int idUnidad, String unidad, String simbolo) {}
