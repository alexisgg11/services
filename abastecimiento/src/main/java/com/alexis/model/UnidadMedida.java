package com.alexis.model;
import jakarta.persistence.*;

@Entity
@Table(name = "unidad_medida", schema = "panaderia")
public class UnidadMedida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_unidad")
    private Integer idUnidad;

    @Column(nullable = false, length = 50, unique = true)
    private String descripcion;

    @Column(nullable = false, length = 10, unique = true)
    private String simbolo;

    protected UnidadMedida() {}

    public UnidadMedida(Integer idUnidad, String descripcion, String simbolo) {
        this.idUnidad = idUnidad;
        this.descripcion = descripcion;
        this.simbolo = simbolo;
    }

    public Integer getIdUnidad() { return idUnidad; }
    public String getDescripcion() { return descripcion; }
    public String getSimbolo() { return simbolo; }
}
