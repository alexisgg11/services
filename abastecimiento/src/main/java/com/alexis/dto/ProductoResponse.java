package com.alexis.dto;
import com.alexis.model.Producto;
import java.math.BigDecimal;

public record ProductoResponse(int idProducto, String descripcion, BigDecimal precio,
    int idTipo, String tipo, int idProveedor, String proveedor, int idUnidad, String unidad, String simbolo) {
    public static ProductoResponse desde(Producto p) {
        return p == null ? null : new ProductoResponse(p.idProducto(), p.descripcion(), p.precio(), p.idTipo(), p.tipo(), p.idProveedor(), p.proveedor(), p.idUnidad(), p.unidad(), p.simbolo());
    }
}
