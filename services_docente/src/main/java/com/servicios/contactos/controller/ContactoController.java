package com.servicios.contactos.controller;

import com.servicios.contactos.dtos.ContactosDTO;
import com.servicios.contactos.model.Contacto;
import com.servicios.contactos.service.ContactoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contactos")
public class ContactoController {

    private final ContactoService contactoService;


    public ContactoController(ContactoService contactoService) {
        this.contactoService = contactoService;
    }

    @GetMapping
    public List<Contacto> listarContactos() {
        return contactoService.listarContactos();
    }

    @GetMapping("/buscar")
    public List<ContactosDTO> buscarContactos(@RequestParam String persona) {
        return contactoService.buscarPorPersona(persona);
    }
    @PostMapping("/insertar/")
    public ContactosDTO insertarContacto(@RequestBody ContactosDTO contacto) {
    return contactoService.insertarContacto(contacto);
    }
}
