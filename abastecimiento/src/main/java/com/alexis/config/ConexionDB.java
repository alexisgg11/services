package com.alexis.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/** Un pool por aplicación; close() devuelve cada conexión al pool. */
public final class ConexionDB implements AutoCloseable {
    private final HikariDataSource pool;
    private final int queryTimeout;

    public ConexionDB(AppConfig config) {
        queryTimeout = config.queryTimeoutSeconds();
        var opciones = new HikariConfig();
        opciones.setJdbcUrl(config.dbUrl());
        opciones.setUsername(config.dbUser());
        opciones.setPassword(config.dbPassword());
        opciones.setMaximumPoolSize(config.poolSize());
        opciones.setMinimumIdle(0);
        opciones.setConnectionTimeout(config.connectionTimeoutSeconds() * 1000L);
        opciones.setValidationTimeout(2000);
        opciones.setIdleTimeout(30000);
        opciones.setInitializationFailTimeout(-1);
        opciones.addDataSourceProperty("connectTimeout", Integer.toString(Math.min(10, config.connectionTimeoutSeconds())));
        opciones.addDataSourceProperty("socketTimeout", "20");
        opciones.addDataSourceProperty("tcpKeepAlive", "true");
        opciones.addDataSourceProperty("options", "-c statement_timeout=" + config.queryTimeoutSeconds() * 1000 + " -c lock_timeout=5000 -c idle_in_transaction_session_timeout=15000");
        pool = new HikariDataSource(opciones);
    }

    public DataSource dataSource() { return pool; }

    public void verificar() throws SQLException {
        try (Connection conexion = pool.getConnection();
             var sentencia = conexion.prepareStatement("SELECT 1 FROM panaderia.producto LIMIT 1")) {
            sentencia.setQueryTimeout(queryTimeout);
            try (var resultado = sentencia.executeQuery()) { /* Verifica conexión, esquema y permisos. */ }
        }
    }

    @Override public void close() { pool.close(); }
}
