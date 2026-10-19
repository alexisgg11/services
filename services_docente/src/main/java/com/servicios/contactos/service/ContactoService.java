package com.servicios.contactos.service;

import com.servicios.contactos.dtos.ContactosDTO;
import com.servicios.contactos.model.Contacto;
import com.servicios.contactos.repository.ContactoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactoService {

    private final ContactoRepository contactoRepository;
    public ContactoService(ContactoRepository contactoRepository) {
        this.contactoRepository = contactoRepository;
    }
    public List<Contacto> listarContactos() {
        return contactoRepository.findAll();
        }
    public List<ContactosDTO> buscarPorPersona(String persona) {
        String termino = persona == null ? "" : persona.trim();
        if (termino.isEmpty()) {
            return List.of();
            }
        List<Contacto>  lista= contactoRepository.findByNombreContainingIgnoreCaseOrApellidosContainingIgnoreCase(termino, termino);
        return  lista.stream().map(contacto -> new ContactosDTO( contacto.getNombre() )).toList();
        }
    
    public ContactosDTO insertarContacto(ContactosDTO dto) {
        
    Contacto contacto = new Contacto();
    contacto.setNombre(dto.getNombre());
    contacto.setApellidos(dto.getApellido());
    contacto.setCelular(dto.getCelular());
    contacto.setDistrito(dto.getDistrito());
    contactoRepository.save(contacto);
    return dto;
    }
    
}

