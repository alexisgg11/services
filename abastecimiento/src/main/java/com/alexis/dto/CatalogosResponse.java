package com.alexis.dto;
import com.alexis.model.*;
import java.util.*;
public record CatalogosResponse(List<Tipo> tipos, List<ProveedorDTO> proveedores, List<Unidad> unidades) {
    public record Tipo(int idTipo, String descripcion) {}
    public record ProveedorDTO(int idProveedor, String ruc, String nombre) {}
    public record Unidad(int idUnidad, String descripcion, String simbolo) {}
    public static CatalogosResponse desde(Map<String,Object> c) {
        return new CatalogosResponse(
            ((List<?>)c.get("tipos")).stream().map(TipoProducto.class::cast).map(t -> new Tipo(t.idTipo(),t.descripcion())).toList(),
            ((List<?>)c.get("proveedores")).stream().map(Proveedor.class::cast).map(p -> new ProveedorDTO(p.idProveedor(),p.ruc(),p.nombre())).toList(),
            ((List<?>)c.get("unidades")).stream().map(UnidadMedida.class::cast).map(u -> new Unidad(u.idUnidad(),u.descripcion(),u.simbolo())).toList());
    }
}
