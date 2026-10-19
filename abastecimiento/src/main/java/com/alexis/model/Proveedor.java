package com.alexis.model;
import jakarta.persistence.*;

@Entity
@Table(name = "proveedor", schema = "panaderia")
public class Proveedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Integer idProveedor;

    @Column(nullable = false, length = 11, unique = true)
    private String ruc;

    @Column(nullable = false, length = 120)
    private String nombre;

    protected Proveedor() {}

    public Proveedor(Integer idProveedor, String ruc, String nombre) {
        this.idProveedor = idProveedor;
        this.ruc = ruc;
        this.nombre = nombre;
    }

    public Integer getIdProveedor() { return idProveedor; }
    public String getRuc() { return ruc; }
    public String getNombre() { return nombre; }
}
