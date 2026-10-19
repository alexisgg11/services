package com.alexis.dto;
import com.alexis.model.Producto;
import java.math.BigDecimal;

public record ProductoResponse(int idProducto, String descripcion, BigDecimal precio,
    int idTipo, String tipo, int idProveedor, String proveedor, int idUnidad, String unidad, String simbolo) {
    public static ProductoResponse desde(Producto p) {
        return p == null ? null : new ProductoResponse(p.getIdProducto(), p.getDescripcion(), p.getPrecio(),
            p.getTipo().getIdTipo(), p.getTipo().getDescripcion(), p.getProveedor().getIdProveedor(), p.getProveedor().getNombre(),
            p.getUnidad().getIdUnidad(), p.getUnidad().getDescripcion(), p.getUnidad().getSimbolo());
    }
}
