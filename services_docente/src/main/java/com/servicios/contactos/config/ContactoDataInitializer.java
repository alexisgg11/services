package com.servicios.contactos.config;

import com.servicios.contactos.model.Contacto;
import com.servicios.contactos.repository.ContactoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ContactoDataInitializer {

    @Bean
    CommandLineRunner cargarContactosDeEjemplo(ContactoRepository contactoRepository) {
        return args -> {
            if (contactoRepository.count() == 0) {
                contactoRepository.saveAll(List.of(
                        new Contacto("Arturo", "Sandoval", "900000001", "Piura"),
                        new Contacto("Juan José", "Sánchez", "900000002", "Las Lomas"),
                        new Contacto("Diego", "Salazar", "900000003", "Talara"),
                        new Contacto("Lucía", "Cárdenas", "900000004", "Sullana"),
                        new Contacto("Mateo", "Vargas", "900000005", "Paita")
                ));
            }
        };
    }
}
