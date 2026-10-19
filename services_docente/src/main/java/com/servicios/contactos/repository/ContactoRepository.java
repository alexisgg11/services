package com.servicios.contactos.repository;

import com.servicios.contactos.model.Contacto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactoRepository extends JpaRepository<Contacto, Integer> {

    List<Contacto> findByNombreContainingIgnoreCaseOrApellidosContainingIgnoreCase(
            String nombre, String apellidos);
}

