package com.alexis.dto;
import com.alexis.model.*;
import java.util.*;
public record CatalogosResponse(List<Tipo> tipos, List<ProveedorDTO> proveedores, List<Unidad> unidades) {
    public record Tipo(int idTipo, String descripcion) {}
    public record ProveedorDTO(int idProveedor, String ruc, String nombre) {}
    public record Unidad(int idUnidad, String descripcion, String simbolo) {}
    public static CatalogosResponse desde(List<TipoProducto> tipos, List<Proveedor> proveedores, List<UnidadMedida> unidades) {
        return new CatalogosResponse(
            tipos.stream().map(t -> new Tipo(t.getIdTipo(), t.getDescripcion())).toList(),
            proveedores.stream().map(p -> new ProveedorDTO(p.getIdProveedor(), p.getRuc(), p.getNombre())).toList(),
            unidades.stream().map(u -> new Unidad(u.getIdUnidad(), u.getDescripcion(), u.getSimbolo())).toList());
    }
}
