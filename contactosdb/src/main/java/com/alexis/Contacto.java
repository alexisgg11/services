package com.alexis;

public class Contacto {
    private int idContactos;
    private String nombre;
    private String apellidos;
    private String celular;
    private String distrito;

    // Constructor (para crear el objeto rápidamente)
    public Contacto(int idContactos, String nombre, String apellidos, String celular, String distrito) {
        this.idContactos = idContactos;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.celular = celular;
        this.distrito = distrito;
    }

    // Método para imprimir el objeto de forma legible
    @Override
    public String toString() {
        return "ID: " + idContactos + " | Alumno: " + nombre + " " + apellidos + " - Cel: " + celular;
    }
    // --- GETTERS (Para obtener los datos) ---
    public int getIdContactos() {
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

    // --- SETTERS (Para modificar los datos si fuera necesario) ---
    public void setIdContactos(int idContactos) {
        this.idContactos = idContactos;
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