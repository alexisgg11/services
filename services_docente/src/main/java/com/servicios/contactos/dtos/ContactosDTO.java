package com.servicios.contactos.dtos;

public class ContactosDTO {

    private String nombre;
    private String apellido;
    private String celular;
    private String distrito;

    public ContactosDTO() {
    }
     public ContactosDTO(String nombre) {
        this.nombre = nombre;
    }
    
       public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getDistrito() {
        return distrito;
    }

    public void setDistrito(String distrito) {
        this.distrito = distrito;
    }
}