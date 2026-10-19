package com.alexis.controller;

import com.alexis.dto.ErrorResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.*;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException error) {
        Map<String,String> campos = new LinkedHashMap<>();
        error.getBindingResult().getFieldErrors().forEach(e -> campos.putIfAbsent(e.getField(),e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorResponse("Revisa los datos: " + String.join(" ", campos.values()),campos));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> argumentos(IllegalArgumentException error) { return respuesta(400,error.getMessage()); }
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> solicitud(Exception error) { return respuesta(400,"La solicitud debe contener todos los campos requeridos y un JSON válido."); }
    @ExceptionHandler({NoSuchElementException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> inexistente(Exception error) { return respuesta(404,"No se encontró el producto o recurso solicitado."); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> restriccion(DataIntegrityViolationException error) {
        return respuesta(409, "Los datos relacionados no son válidos o incumplen una restricción.");
    }
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> baseDatos(DataAccessException error) {
        return respuesta(503, "No se pudo consultar PostgreSQL. Intenta nuevamente.");
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodo(Exception error) { return respuesta(405,"Método no permitido."); }
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> formato(Exception error) { return respuesta(415,"Envía los datos con Content-Type application/json."); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> interno(Exception error) { return respuesta(500,"No se pudo completar la operación. Intenta nuevamente."); }
    private ResponseEntity<ErrorResponse> respuesta(int estado, String mensaje) {
        return ResponseEntity.status(estado).body(new ErrorResponse(mensaje));
    }
}
