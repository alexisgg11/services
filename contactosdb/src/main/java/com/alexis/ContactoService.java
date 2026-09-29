package com.alexis;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ContactoService {

    public List<Contacto> obtenerTodos() {
        List<Contacto> lista = new ArrayList<>();
        String sql = "SELECT * FROM contactos"; 

        try {
            // Usar tu puente para conectar
            Connection conn = ConexionDB.conectar();
            // Preparar el vehículo que llevará la consulta
            Statement stmt = conn.createStatement();
            // Ejecutar y traer los resultados (ResultSet)
            ResultSet rs = stmt.executeQuery(sql);

            // Recorrer fila por fila lo que devolvió Postgres
            while (rs.next()) {
                // Empaquetar cada fila en el molde (Modelo) que creaste antes
                Contacto c = new Contacto(
                    rs.getInt("id_contactos"),
                    rs.getString("nombre"),
                    rs.getString("apellidos"),
                    rs.getString("celular"),
                    rs.getString("distrito")
                );
                // Guardarlo en la lista
                lista.add(c);
            }
            
            // Cerrar recursos y delegar la desconexión
            rs.close();
            stmt.close();
            ConexionDB.desconectar(conn);
            
        } catch (Exception e) {
            System.err.println("Error al obtener datos: " + e.getMessage());
        }
        
        return lista; // Devolver la lista llena
    }
    
    public boolean registrar(Contacto contacto) {
        // Usamos signos de interrogación como marcadores de posición
        String sql = "INSERT INTO contactos (nombre, apellidos, celular, distrito) VALUES (?, ?, ?, ?)";
        boolean exito = false;

        try {
            // 1. Abrir conexión
            Connection conn = ConexionDB.conectar();
            
            // 2. Preparar la consulta segura (PreparedStatement)
            java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
            
            // 3. Reemplazar los "?" con los datos reales del objeto
            pstmt.setString(1, contacto.getNombre());
            pstmt.setString(2, contacto.getApellidos());
            pstmt.setString(3, contacto.getCelular());
            pstmt.setString(4, contacto.getDistrito());

            // 4. Ejecutar la inserción en la base de datos
            pstmt.executeUpdate();
            exito = true; // Si llegó hasta aquí, no hubo errores

            // 5. Cerrar recursos
            pstmt.close();
            ConexionDB.desconectar(conn);
            
        } catch (Exception e) {
            System.err.println("Error al registrar el contacto: " + e.getMessage());
        }
        
        return exito;
    }
}