package com.alexis.dto;

import jakarta.validation.constraints.*;

public record ProductoRequest(
    @NotBlank(message="La descripción es obligatoria.")
    @Size(max=150, message="La descripción admite hasta 150 caracteres.") String descripcion,
    @NotBlank(message="El precio es obligatorio.")
    @Pattern(regexp="[0-9]{1,8}(\\.[0-9]{1,2})?", message="El precio debe usar punto decimal y hasta dos decimales.") String precio,
    @NotBlank(message="Selecciona un tipo de producto.") @Pattern(regexp="[1-9][0-9]*", message="El tipo debe ser un ID positivo.") String idTipo,
    @NotBlank(message="Selecciona un proveedor.") @Pattern(regexp="[1-9][0-9]*", message="El proveedor debe ser un ID positivo.") String idProveedor,
    @NotBlank(message="Selecciona una unidad de medida.") @Pattern(regexp="[1-9][0-9]*", message="La unidad debe ser un ID positivo.") String idUnidad) {}
