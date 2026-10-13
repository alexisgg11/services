package com.alexis;

import tools.jackson.databind.json.JsonMapper;
import com.alexis.model.PaginaProductos;
import com.alexis.model.Producto;
import com.alexis.repository.ProductoRepository;
import com.alexis.service.ProductoService;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ProductoServiceTest {
    private static class Repo extends ProductoRepository {
        Repo() { super(null, 10); }
        BigDecimal precio; String descripcion; Integer proveedor; Integer tipo; boolean guardar;
        @Override public int guardar(Integer id, String d, BigDecimal p, int t, int v, int u) {
            guardar = true; descripcion = d; precio = p; return id == null ? 51 : 0;
        }
        @Override public PaginaProductos listar(String b, Integer p, Integer t, int pagina, int tamano) { proveedor=p; tipo=t; return new PaginaProductos(List.of(), 0, 1, tamano, 1); }
        @Override public Optional<Producto> masCaro() { return Optional.empty(); }
        @Override public boolean eliminar(int id) { return false; }
    }
    private Map<String,String> datos(String precio) {
        return Map.of("descripcion"," Harina ","precio",precio,"idTipo","1","idProveedor","1","idUnidad","1");
    }
    @Test public void normalizaPrecioSinPerderDecimales() throws Exception {
        var repo = new Repo(); var service = new ProductoService(repo);
        assertEquals(51, service.guardar(null, datos("4.80")));
        assertEquals(new BigDecimal("4.80"), repo.precio); assertEquals("Harina",repo.descripcion);
    }
    @Test public void rechazaPreciosInvalidosAntesDePersistir() throws Exception {
        for (String precio : List.of("-1", "1.001", "1.000", "100000000", "NaN", "", "4,80", "1e2", "+4", " 4.80 ")) {
            var repo = new Repo();
            try { new ProductoService(repo).guardar(null, datos(precio)); fail("Aceptó " + precio); }
            catch (IllegalArgumentException expected) { assertFalse(repo.guardar); }
        }
    }
    @Test public void restriccionesSeAplicanTambienAlEditar() throws Exception {
        for (String descripcion : List.of(" ", "x".repeat(151), "Harina\nTrigo", "Harina\u0000")) {
            var data = new HashMap<>(datos("4.80")); data.put("descripcion", descripcion);
            var repo = new Repo();
            try { new ProductoService(repo).guardar(1, data); fail("Descripción inválida aceptada al editar"); }
            catch (IllegalArgumentException expected) { assertFalse(repo.guardar); }
        }
        var repo = new Repo();
        try { new ProductoService(repo).guardar(1, datos("1e2")); fail("Precio inválido aceptado al editar"); }
        catch (IllegalArgumentException expected) { assertFalse(repo.guardar); }
    }
    @Test public void aceptaLimitesDePrecioYDescripcion() throws Exception {
        for (String precio : List.of("0", "0.00", "99999999.99")) {
            var data = new HashMap<>(datos(precio)); data.put("descripcion", "x".repeat(150));
            var repo = new Repo();
            new ProductoService(repo).guardar(null, data);
            assertTrue(repo.guardar); assertEquals(new BigDecimal(precio).setScale(2), repo.precio);
        }
    }
    @Test public void rechazaDescripcionVaciaYClavesInvalidas() throws Exception {
        for (String clave : List.of("descripcion", "idTipo", "idProveedor", "idUnidad")) {
            var datos = new HashMap<>(datos("1.00")); datos.put(clave, "");
            var repo = new Repo();
            try { new ProductoService(repo).guardar(null, datos); fail("Aceptó " + clave); }
            catch (IllegalArgumentException expected) { assertFalse(repo.guardar); }
        }
    }
    @Test public void serviciosUsanElFiltroCorrecto() throws Exception {
        var repo = new Repo(); var service = new ProductoService(repo);
        service.porProveedor(3, 1, 10); assertEquals(Integer.valueOf(3), repo.proveedor); assertNull(repo.tipo);
        service.porTipo(4, 1, 10); assertEquals(Integer.valueOf(4), repo.tipo); assertNull(repo.proveedor);
    }
    @Test public void catalogoVacioDevuelveNoEncontrado() throws Exception {
        assertThrows(NoSuchElementException.class, () -> new ProductoService(new Repo()).masCaro());
    }
    @Test public void actualizarInexistenteDevuelveNoEncontrado() throws Exception {
        assertThrows(NoSuchElementException.class, () -> new ProductoService(new Repo()).guardar(999, datos("4.80")));
    }
    @Test public void eliminarInexistenteDevuelveNoEncontrado() throws Exception {
        assertThrows(NoSuchElementException.class, () -> new ProductoService(new Repo()).eliminar(999));
    }
    @Test public void jsonEscapaCaracteresYConservaDecimal() {
        assertEquals("\"a\\\"b\\\\c\\n\\u0001\"", JsonMapper.builder().build().writeValueAsString("a\"b\\c\n\u0001"));
        var producto = new Producto(1,"Harina", new BigDecimal("4.80"),1,"Básicos",1,"Molino",1,"Kilogramo","kg");
        assertTrue(JsonMapper.builder().build().writeValueAsString(producto).contains("\"precio\":4.80"));
        assertTrue(JsonMapper.builder().build().writeValueAsString(producto).contains("\"descripcion\":\"Harina\""));
    }
    @Test public void rechazaPaginacionFueraDeRango() throws Exception {
        var service = new ProductoService(new Repo());
        for (int[] valores : List.of(new int[]{0,10}, new int[]{1,0}, new int[]{1,101}, new int[]{1000001,10})) {
            try { service.listar("",null,null,valores[0],valores[1]); fail("Paginación inválida aceptada"); }
            catch (IllegalArgumentException expected) {}
        }
    }
    @Test public void rechazaBusquedasExtensas() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new ProductoService(new Repo()).listar("x".repeat(151),null,null,1,10));
    }
}
