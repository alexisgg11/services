package com.alexis;

import java.util.List;
import java.util.Scanner; // Importamos la herramienta para leer texto

public class App {
    public static void main(String[] args) {
        ContactoService servicio = new ContactoService();
        // 1. Inicializar el lector de teclado
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== REGISTRO DE NUEVO ALUMNO ===");
        
        // 2. Pedir los datos uno por uno
        System.out.print("Ingrese el nombre: ");
        String nombre = scanner.nextLine();
        
        System.out.print("Ingrese los apellidos: ");
        String apellidos = scanner.nextLine();
        
        System.out.print("Ingrese el celular (9 dígitos): ");
        String celular = scanner.nextLine();
        
        System.out.print("Ingrese el distrito: ");
        String distrito = scanner.nextLine();

        // 3. Crear el objeto Contacto con las variables que el usuario acaba de escribir
        Contacto nuevoAlumno = new Contacto(0, nombre, apellidos, celular, distrito);
        
        // 4. Enviar el objeto al servicio para registrarlo en la BD
        System.out.println("\nGuardando en la base de datos...");
        boolean registrado = servicio.registrar(nuevoAlumno);
        
        if (registrado) {
            System.out.println("¡Alumno registrado con éxito!\n");
        } else {
            System.out.println("Hubo un problema al registrar.\n");
        }

        // 5. Mostrar todos los alumnos para confirmar
        List<Contacto> misAlumnos = servicio.obtenerTodos();
        System.out.println("=== LISTA DE ALUMNOS ACTUALIZADA ===");
        for (Contacto alumno : misAlumnos) {
            System.out.println(alumno.toString());
        }
        
        // 6. Cerrar el lector
        scanner.close();
    }
}