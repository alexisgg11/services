package com.alexis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.alexis.util.DiagnosticoArranque;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        try {
            SpringApplication.run(App.class, args);
        } catch (Exception error) {
            System.err.println(DiagnosticoArranque.mensajeError(error));
            System.exit(1);
        }
    }
}
