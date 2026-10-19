package com.alexis.model;
import jakarta.persistence.*;

@Entity
@Table(name = "tipo_producto", schema = "panaderia")
public class TipoProducto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo")
    private Integer idTipo;

    @Column(nullable = false, length = 80, unique = true)
    private String descripcion;

    protected TipoProducto() {}

    public TipoProducto(Integer idTipo, String descripcion) {
        this.idTipo = idTipo;
        this.descripcion = descripcion;
    }

    public Integer getIdTipo() { return idTipo; }
    public String getDescripcion() { return descripcion; }
}
