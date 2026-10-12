package com.alexis;

import com.alexis.config.AppConfig;
import com.alexis.config.ConexionDB;
import com.alexis.controller.ProductoController;
import com.alexis.repository.ProductoRepository;
import com.alexis.service.ProductoService;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
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
            System.err.println("No se pudo iniciar la aplicación. Revisa la configuración, PostgreSQL y el puerto.");
            System.exit(1);
        }
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
