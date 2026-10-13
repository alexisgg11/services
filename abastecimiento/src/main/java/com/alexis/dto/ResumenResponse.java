package com.alexis.dto;
import com.alexis.model.Resumen;
import java.util.Map;
public record ResumenResponse(long productos, ProductoResponse masCaro, Map<Integer,Long> productosPorProveedor) {
    public static ResumenResponse desde(Resumen r) {
        return new ResumenResponse(r.productos(), ProductoResponse.desde(r.masCaro()), r.productosPorProveedor());
    }
}
