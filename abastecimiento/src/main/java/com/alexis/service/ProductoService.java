package com.alexis.service;

import com.alexis.dto.*;
import com.alexis.model.*;
import com.alexis.repository.*;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ProductoService {
    private final ProductoRepository productos;
    private final TipoProductoRepository tipos;
    private final ProveedorRepository proveedores;
    private final UnidadMedidaRepository unidades;

    public ProductoService(ProductoRepository productos, TipoProductoRepository tipos,
                           ProveedorRepository proveedores, UnidadMedidaRepository unidades) {
        this.productos = productos;
        this.tipos = tipos;
        this.proveedores = proveedores;
        this.unidades = unidades;
    }

    public PaginaProductos listar(String buscar, Integer proveedor, Integer tipo, int pagina, int tamano) {
        if (buscar.length() > 150) throw new IllegalArgumentException("La búsqueda admite hasta 150 caracteres.");
        if (pagina < 1 || pagina > 1000000 || tamano < 1 || tamano > 100)
            throw new IllegalArgumentException("Página o tamaño fuera de rango.");
        if (proveedor != null && proveedor < 1 || tipo != null && tipo < 1)
            throw new IllegalArgumentException("El filtro debe ser un ID positivo.");

        var resultado = productos.buscar(buscar.strip(), proveedor, tipo, pagina(pagina, tamano));
        int paginas = Math.max(1, resultado.getTotalPages());
        int actual = Math.min(pagina, paginas);
        if (actual != pagina) resultado = productos.buscar(buscar.strip(), proveedor, tipo, pagina(actual, tamano));
        return new PaginaProductos(resultado.getContent(), resultado.getTotalElements(), actual, tamano, paginas);
    }

    private PageRequest pagina(int numero, int tamano) {
        return PageRequest.of(numero - 1, tamano, Sort.by("idProducto"));
    }

    public Producto obtener(int id) {
        return productos.findByIdProductoAndEstadoTrue(id).orElseThrow(() -> new NoSuchElementException("Producto no encontrado."));
    }

    public Producto masCaro() {
        return productos.findFirstByEstadoTrueOrderByPrecioDescIdProductoAsc()
            .orElseThrow(() -> new NoSuchElementException("No hay productos registrados."));
    }

    public PaginaProductos porProveedor(int id, int pagina, int tamano) { return listar("", id, null, pagina, tamano); }
    public PaginaProductos porTipo(int id, int pagina, int tamano) { return listar("", null, id, pagina, tamano); }

    public Resumen resumen() {
        var cantidades = productos.contarPorProveedor(PageRequest.of(0, 101));
        if (cantidades.size() > 100) throw new DataAccessResourceFailureException("El catálogo supera el límite permitido.");
        Map<Integer, Long> porProveedor = new LinkedHashMap<>();
        cantidades.forEach(fila -> porProveedor.put(fila.getIdProveedor(), fila.getCantidad()));
        long total = porProveedor.values().stream().mapToLong(Long::longValue).sum();
        return new Resumen(total, productos.findFirstByEstadoTrueOrderByPrecioDescIdProductoAsc().orElse(null), porProveedor);
    }

    public CatalogosResponse catalogos() {
        var tiposPagina = tipos.findAll(PageRequest.of(0, 100, Sort.by("idTipo")));
        var proveedoresPagina = proveedores.findAll(PageRequest.of(0, 100, Sort.by("idProveedor")));
        var unidadesPagina = unidades.findAll(PageRequest.of(0, 100, Sort.by("idUnidad")));
        if (tiposPagina.getTotalElements() > 100 || proveedoresPagina.getTotalElements() > 100 || unidadesPagina.getTotalElements() > 100)
            throw new DataAccessResourceFailureException("El catálogo supera el límite permitido.");
        return CatalogosResponse.desde(tiposPagina.getContent(), proveedoresPagina.getContent(), unidadesPagina.getContent());
    }

    public boolean saludable() { return productos.count() >= 0; }

    @Transactional
    public int guardar(Integer id, ProductoRequest datos) {
        String descripcion = datos.descripcion() == null ? "" : datos.descripcion().strip();
        if (descripcion.isBlank() || descripcion.length() > 150)
            throw new IllegalArgumentException("La descripción debe tener entre 1 y 150 caracteres.");
        if (descripcion.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("La descripción no admite caracteres de control ni saltos de línea.");
        String textoPrecio = datos.precio();
        if (textoPrecio == null || !textoPrecio.matches("[0-9]{1,8}(\\.[0-9]{1,2})?"))
            throw new IllegalArgumentException("El precio debe estar entre 0 y 99999999.99, con hasta dos decimales y punto decimal.");
        BigDecimal precio = new BigDecimal(textoPrecio).setScale(2);
        int tipo = idPositivo(datos.idTipo());
        int proveedor = idPositivo(datos.idProveedor());
        int unidad = idPositivo(datos.idUnidad());

        TipoProducto referenciaTipo = tipos.getReferenceById(tipo);
        Proveedor referenciaProveedor = proveedores.getReferenceById(proveedor);
        UnidadMedida referenciaUnidad = unidades.getReferenceById(unidad);
        Producto producto;
        if (id == null) producto = new Producto(descripcion, precio, referenciaTipo, referenciaProveedor, referenciaUnidad);
        else {
            producto = obtener(id);
            producto.actualizar(descripcion, precio, referenciaTipo, referenciaProveedor, referenciaUnidad);
        }
        return productos.saveAndFlush(producto).getIdProducto();
    }

    @Transactional
    public void eliminar(int id) {
        Producto producto = obtener(id);
        producto.desactivar();
        productos.saveAndFlush(producto);
    }

    public static int idPositivo(String valor) {
        try {
            int id = Integer.parseInt(valor);
            if (id > 0) return id;
        } catch (NumberFormatException ignored) {}
        throw new IllegalArgumentException("Selecciona un ID entero positivo.");
    }
}
