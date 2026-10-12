package com.alexis;

import com.alexis.config.AppConfig;
import com.alexis.config.ConexionDB;
import com.alexis.controller.ProductoController;
import com.alexis.repository.ProductoRepository;
import com.alexis.service.ProductoService;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.BindException;
import java.net.UnknownHostException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class App {
    public static void main(String[] args) {
        try {
            iniciar(AppConfig.cargar());
        } catch (IllegalArgumentException error) {
            System.err.println(error.getMessage());
            System.exit(1);
        } catch (Exception error) {
            System.err.println(mensajeError(error));
            System.exit(1);
        }
    }

    static String mensajeError(Throwable error) {
        // Se muestran causas conocidas, nunca mensajes del driver con datos privados.
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLException sql) {
                String estado = sql.getSQLState();
                if ("28P01".equals(estado) || "28000".equals(estado))
                    return "PostgreSQL rechazó el usuario o la contraseña. Revisa DB_USER y DB_PASSWORD del archivo seleccionado.";
                if ("42P01".equals(estado) || "3F000".equals(estado))
                    return "Falta el esquema panaderia o la tabla producto en la base seleccionada. Ejecuta el script de tablas y datos en esa base.";
                if ("3D000".equals(estado)) return "La base indicada en DB_URL no existe.";
                if ("42501".equals(estado)) return "El usuario no tiene permisos para consultar el esquema panaderia.";
            }
            if (causa instanceof BindException)
                return "No se pudo abrir el servidor: el puerto está ocupado o APP_HOST no está disponible. Detén el otro servidor o revisa APP_HOST y PORT.";
            if (causa instanceof UnknownHostException)
                return "No se pudo resolver el servidor de PostgreSQL. Revisa el host de DB_URL y la conexión a Internet.";
        }
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof SocketTimeoutException || causa instanceof java.sql.SQLTransientConnectionException)
                return "Se agotó el tiempo de conexión con PostgreSQL. Revisa que la base esté activa, la red y DB_CONNECTION_TIMEOUT_SECONDS.";
            if (causa instanceof ConnectException)
                return "No se pudo conectar con PostgreSQL. Revisa que esté activo y que el host y puerto de DB_URL sean correctos.";
        }
        return "No se pudo iniciar la aplicación. Revisa la conexión a PostgreSQL, APP_HOST y PORT del archivo seleccionado.";
    }

    private static void iniciar(AppConfig config) throws Exception {
        var base = new ConexionDB(config);
        try {
            base.verificar();
            var servidor = HttpServer.create(new InetSocketAddress(config.host(), config.port()), 32);
            var ejecutor = Executors.newFixedThreadPool(config.httpThreads());
            servidor.setExecutor(ejecutor);
            var repository = new ProductoRepository(base.dataSource(), config.queryTimeoutSeconds());
            servidor.createContext("/", new ProductoController(new ProductoService(repository)));
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                servidor.stop(2);
                ejecutor.shutdown();
                try {
                    if (!ejecutor.awaitTermination(5, TimeUnit.SECONDS)) ejecutor.shutdownNow();
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    ejecutor.shutdownNow();
                } finally { base.close(); }
            }));
            servidor.start();
            System.out.println("Servidor iniciado en " + servidor.getAddress());
        } catch (Exception error) {
            base.close();
            throw error;
        }
    }
}
