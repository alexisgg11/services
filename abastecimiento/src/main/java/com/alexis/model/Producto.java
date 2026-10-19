package com.alexis.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "producto", schema = "panaderia")
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(nullable = false, length = 150)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo", nullable = false)
    private TipoProducto tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_unidad", nullable = false)
    private UnidadMedida unidad;

    protected Producto() {}

    public Producto(String descripcion, BigDecimal precio, TipoProducto tipo, Proveedor proveedor, UnidadMedida unidad) {
        actualizar(descripcion, precio, tipo, proveedor, unidad);
    }

    public void actualizar(String descripcion, BigDecimal precio, TipoProducto tipo, Proveedor proveedor, UnidadMedida unidad) {
        this.descripcion = descripcion;
        this.precio = precio;
        this.tipo = tipo;
        this.proveedor = proveedor;
        this.unidad = unidad;
    }

    public Integer getIdProducto() { return idProducto; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getPrecio() { return precio; }
    public TipoProducto getTipo() { return tipo; }
    public Proveedor getProveedor() { return proveedor; }
    public UnidadMedida getUnidad() { return unidad; }
}
