package com.servicios.contactos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contactos")
public class Contacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contactos")
    private Integer idContactos;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "apellidos", nullable = false, length = 150)
    private String apellidos;

    @Column(name = "celular", nullable = false, length = 9)
    private String celular;

    @Column(name = "distrito", nullable = false, length = 50)
    private String distrito;

    public  Contacto() {
    }

    public Contacto(String nombre, String apellidos,
                    String celular, String distrito) {
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.celular = celular;
        this.distrito = distrito;
    }

    public Integer getIdContactos() {
        return idContactos;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getCelular() {
        return celular;
    }

    public String getDistrito() {
        return distrito;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public void setDistrito(String distrito) {
        this.distrito = distrito;
    }
}
