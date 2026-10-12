package com.alexis.service;

import com.alexis.model.*;
import com.alexis.repository.ProductoRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

public class ProductoService {
    private final ProductoRepository repository;
    public ProductoService(ProductoRepository repository) { this.repository = repository; }
    public PaginaProductos listar(String buscar, Integer proveedor, Integer tipo, int pagina, int tamano) throws SQLException {
        if (buscar.length() > 150) throw new IllegalArgumentException("La búsqueda admite hasta 150 caracteres.");
        if (pagina < 1 || pagina > 1000000 || tamano < 1 || tamano > 100)
            throw new IllegalArgumentException("Página o tamaño fuera de rango.");
        if (proveedor != null && proveedor < 1 || tipo != null && tipo < 1)
            throw new IllegalArgumentException("El filtro debe ser un ID positivo.");
        return repository.listar(buscar.strip(), proveedor, tipo, pagina, tamano);
    }
    public Producto obtener(int id) throws SQLException {
        return repository.obtener(id).orElseThrow(() -> new NoSuchElementException("Producto no encontrado."));
    }
    public Producto masCaro() throws SQLException {
        return repository.masCaro().orElseThrow(() -> new NoSuchElementException("No hay productos registrados."));
    }
    public PaginaProductos porProveedor(int id, int pagina, int tamano) throws SQLException { return listar("", id, null, pagina, tamano); }
    public PaginaProductos porTipo(int id, int pagina, int tamano) throws SQLException { return listar("", null, id, pagina, tamano); }
    public Resumen resumen() throws SQLException { return repository.resumen(); }
    public Map<String, Object> catalogos() throws SQLException { return repository.catalogos(); }
    public boolean saludable() throws SQLException { return repository.saludable(); }

    public int guardar(Integer id, Map<String, String> datos) throws SQLException {
        String descripcion = datos.getOrDefault("descripcion", "").strip();
        if (descripcion.isBlank() || descripcion.length() > 150)
            throw new IllegalArgumentException("La descripción debe tener entre 1 y 150 caracteres.");
        if (descripcion.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("La descripción no admite caracteres de control ni saltos de línea.");
        String textoPrecio = datos.getOrDefault("precio", "");
        if (!textoPrecio.matches("[0-9]{1,8}(\\.[0-9]{1,2})?"))
            throw new IllegalArgumentException("El precio debe estar entre 0 y 99999999.99, con hasta dos decimales y punto decimal.");
        BigDecimal precio;
        try { precio = new BigDecimal(textoPrecio); }
        catch (NumberFormatException error) { throw new IllegalArgumentException("El precio debe ser decimal."); }
        if (precio.signum() < 0 || precio.compareTo(new BigDecimal("99999999.99")) > 0
                || precio.stripTrailingZeros().scale() > 2)
            throw new IllegalArgumentException("El precio debe ser positivo o cero, con hasta dos decimales.");
        int resultado = repository.guardar(id, descripcion, precio.setScale(2),
            idPositivo(datos.get("idTipo")), idPositivo(datos.get("idProveedor")), idPositivo(datos.get("idUnidad")));
        if (resultado == 0) throw new NoSuchElementException("Producto no encontrado.");
        return resultado;
    }
    public void eliminar(int id) throws SQLException {
        if (!repository.eliminar(id)) throw new NoSuchElementException("Producto no encontrado.");
    }
    public static int idPositivo(String valor) {
        try {
            int id = Integer.parseInt(valor);
            if (id > 0) return id;
        } catch (NumberFormatException ignored) {}
        throw new IllegalArgumentException("Selecciona un ID entero positivo.");
    }
}
