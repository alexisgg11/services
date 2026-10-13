package com.alexis;

import java.net.*;
import java.sql.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AppTest {
    @Test public void distingueAutenticacionEnErrorEnvueltoSinMostrarSecretos() {
        var passwordError = new SQLException("contraseña-privada jdbc:postgresql://privado", "28P01");
        var poolError = new SQLTransientConnectionException("pool agotado", "08001", passwordError);
        String message = com.alexis.util.DiagnosticoArranque.mensajeError(poolError);
        assertTrue(message.contains("DB_PASSWORD"));
        assertFalse(message.contains("contraseña-privada"));
        assertFalse(message.contains("jdbc:postgresql"));
    }
    @Test public void distingueEsquemaPermisosYPuerto() {
        assertTrue(com.alexis.util.DiagnosticoArranque.mensajeError(new SQLException("privado", "42P01")).contains("tabla producto"));
        assertTrue(com.alexis.util.DiagnosticoArranque.mensajeError(new SQLException("privado", "42501")).contains("permisos"));
        assertTrue(com.alexis.util.DiagnosticoArranque.mensajeError(new BindException("privado")).contains("puerto"));
    }
    @Test public void distingueDnsYTiempoDeConexion() {
        assertTrue(com.alexis.util.DiagnosticoArranque.mensajeError(new SQLException("privado", new UnknownHostException("privado"))).contains("resolver"));
        assertTrue(com.alexis.util.DiagnosticoArranque.mensajeError(new SQLTransientConnectionException("privado")).contains("DB_CONNECTION_TIMEOUT_SECONDS"));
        assertTrue(com.alexis.util.DiagnosticoArranque.mensajeError(new ConnectException("privado")).contains("host y puerto"));
    }
}
