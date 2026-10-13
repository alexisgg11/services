package com.alexis.controller;

import com.alexis.dto.*;
import com.alexis.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.sql.SQLException;
import java.util.Map;

@RestController
@RequestMapping(value="/api", produces=MediaType.APPLICATION_JSON_VALUE)
public class ProductoController {
    private final ProductoService servicio;
    public ProductoController(ProductoService servicio) { this.servicio = servicio; }

    @GetMapping("/health") public ResponseEntity<Map<String,String>> health() throws SQLException {
        boolean ok = servicio.saludable();
        return ResponseEntity.status(ok ? 200 : 503).body(Map.of("estado", ok ? "ok" : "error", "baseDatos", "PostgreSQL"));
    }
    @GetMapping("/catalogos") public CatalogosResponse catalogos() throws SQLException { return CatalogosResponse.desde(servicio.catalogos()); }
    @GetMapping("/resumen") public ResumenResponse resumen() throws SQLException { return ResumenResponse.desde(servicio.resumen()); }
    @GetMapping("/productos") public PaginaProductosResponse listar(
        @RequestParam(defaultValue="") String buscar, @RequestParam(required=false) String proveedor,
        @RequestParam(required=false) String tipo, @RequestParam(defaultValue="1") String pagina,
        @RequestParam(defaultValue="10") String tamano) throws SQLException {
        return PaginaProductosResponse.desde(servicio.listar(buscar, opcional(proveedor), opcional(tipo), positivo(pagina), positivo(tamano)));
    }
    @GetMapping("/productos/{id}") public ProductoResponse obtener(@PathVariable String id) throws SQLException {
        return ProductoResponse.desde(servicio.obtener(positivo(id)));
    }
    @PostMapping(value="/productos", consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OperacionResponse> registrar(@Valid @RequestBody ProductoRequest datos) throws SQLException {
        int id = servicio.guardar(null, datos.datos());
        return ResponseEntity.created(URI.create("/api/productos/" + id)).body(new OperacionResponse(id,"Producto registrado correctamente."));
    }
    @PutMapping(value="/productos/{id}", consumes=MediaType.APPLICATION_JSON_VALUE)
    public OperacionResponse editar(@PathVariable String id, @Valid @RequestBody ProductoRequest datos) throws SQLException {
        int resultado = servicio.guardar(positivo(id), datos.datos());
        return new OperacionResponse(resultado,"Producto actualizado correctamente.");
    }
    @DeleteMapping("/productos/{id}") public OperacionResponse eliminar(@PathVariable String id) throws SQLException {
        servicio.eliminar(positivo(id)); return new OperacionResponse(null,"Producto eliminado correctamente.");
    }
    @GetMapping("/servicios/producto-mas-caro") public ProductoResponse masCaro() throws SQLException { return ProductoResponse.desde(servicio.masCaro()); }
    @GetMapping("/servicios/por-proveedor") public PaginaProductosResponse porProveedor(@RequestParam String id,
        @RequestParam(defaultValue="1") String pagina, @RequestParam(defaultValue="10") String tamano) throws SQLException {
        return PaginaProductosResponse.desde(servicio.porProveedor(positivo(id),positivo(pagina),positivo(tamano)));
    }
    @GetMapping("/servicios/por-tipo") public PaginaProductosResponse porTipo(@RequestParam String id,
        @RequestParam(defaultValue="1") String pagina, @RequestParam(defaultValue="10") String tamano) throws SQLException {
        return PaginaProductosResponse.desde(servicio.porTipo(positivo(id),positivo(pagina),positivo(tamano)));
    }
    private static int positivo(String valor) { return ProductoService.idPositivo(valor); }
    private static Integer opcional(String valor) { return valor == null || valor.isBlank() ? null : positivo(valor); }
}