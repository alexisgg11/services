package com.alexis;

import com.alexis.dto.*;
import com.alexis.model.*;
import com.alexis.repository.*;
import com.alexis.service.ProductoService;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductoServiceTest {
    private final ProductoRepository productos = mock(ProductoRepository.class);
    private final TipoProductoRepository tipos = mock(TipoProductoRepository.class);
    private final ProveedorRepository proveedores = mock(ProveedorRepository.class);
    private final UnidadMedidaRepository unidades = mock(UnidadMedidaRepository.class);
    private final ProductoService service = new ProductoService(productos, tipos, proveedores, unidades);

    private ProductoRequest datos(String precio) {
        return new ProductoRequest(" Harina ", precio, "1", "1", "1");
    }

    private void prepararGuardado() {
        when(tipos.getReferenceById(1)).thenReturn(new TipoProducto(1, "Básicos"));
        when(proveedores.getReferenceById(1)).thenReturn(new Proveedor(1, "20000000001", "Molino"));
        when(unidades.getReferenceById(1)).thenReturn(new UnidadMedida(1, "Kilogramo", "kg"));
        when(productos.saveAndFlush(any(Producto.class))).thenAnswer(invocation -> {
            Producto producto = invocation.getArgument(0);
            ReflectionTestUtils.setField(producto, "idProducto", 51);
            return producto;
        });
    }

    @Test void normalizaPrecioSinPerderDecimales() {
        prepararGuardado();
        assertEquals(51, service.guardar(null, datos("4.80")));
        verify(productos).saveAndFlush(argThat(p -> p.getPrecio().equals(new BigDecimal("4.80")) && p.getDescripcion().equals("Harina")));
    }

    @Test void rechazaPreciosInvalidosAntesDePersistir() {
        for (String precio : List.of("-1", "1.001", "1.000", "100000000", "NaN", "", "4,80", "1e2", "+4", " 4.80 "))
            assertThrows(IllegalArgumentException.class, () -> service.guardar(null, datos(precio)));
        verifyNoInteractions(productos, tipos, proveedores, unidades);
    }

    @Test void restriccionesSeAplicanTambienAlEditar() {
        for (String descripcion : List.of(" ", "x".repeat(151), "Harina\nTrigo", "Harina\u0000")) {
            var datos = new ProductoRequest(descripcion, "4.80", "1", "1", "1");
            assertThrows(IllegalArgumentException.class, () -> service.guardar(1, datos));
        }
        assertThrows(IllegalArgumentException.class, () -> service.guardar(1, datos("1e2")));
        verifyNoInteractions(productos, tipos, proveedores, unidades);
    }

    @Test void aceptaLimitesDePrecioYDescripcion() {
        prepararGuardado();
        for (String precio : List.of("0", "0.00", "99999999.99"))
            assertEquals(51, service.guardar(null, new ProductoRequest("x".repeat(150), precio, "1", "1", "1")));
        verify(productos, times(3)).saveAndFlush(any(Producto.class));
    }

    @Test void rechazaDescripcionVaciaYClavesInvalidas() {
        List<ProductoRequest> invalidos = List.of(
            new ProductoRequest("", "1.00", "1", "1", "1"),
            new ProductoRequest("Harina", "1.00", "", "1", "1"),
            new ProductoRequest("Harina", "1.00", "1", "", "1"),
            new ProductoRequest("Harina", "1.00", "1", "1", ""));
        invalidos.forEach(datos -> assertThrows(IllegalArgumentException.class, () -> service.guardar(null, datos)));
        verifyNoInteractions(productos, tipos, proveedores, unidades);
    }

    @Test void serviciosUsanElFiltroCorrecto() {
        when(productos.buscar(anyString(), nullable(Integer.class), nullable(Integer.class), any(Pageable.class)))
            .thenReturn(Page.empty());
        service.porProveedor(3, 1, 10);
        verify(productos).buscar(eq(""), eq(3), isNull(), any(Pageable.class));
        service.porTipo(4, 1, 10);
        verify(productos).buscar(eq(""), isNull(), eq(4), any(Pageable.class));
    }

    @Test void catalogoVacioDevuelveNoEncontrado() {
        when(productos.findFirstByOrderByPrecioDescIdProductoAsc()).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, service::masCaro);
    }

    @Test void actualizarInexistenteDevuelveNoEncontrado() {
        when(productos.findById(999)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.guardar(999, datos("4.80")));
        verify(productos, never()).saveAndFlush(any(Producto.class));
    }

    @Test void eliminarInexistenteDevuelveNoEncontrado() {
        when(productos.findById(999)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.eliminar(999));
        verify(productos, never()).delete(any(Producto.class));
    }

    @Test void jsonEscapaCaracteresYConservaDecimal() {
        var mapper = JsonMapper.builder().build();
        assertEquals("\"a\\\"b\\\\c\\n\\u0001\"", mapper.writeValueAsString("a\"b\\c\n\u0001"));
        var producto = new Producto("Harina", new BigDecimal("4.80"), new TipoProducto(1, "Básicos"),
            new Proveedor(1, "20000000001", "Molino"), new UnidadMedida(1, "Kilogramo", "kg"));
        ReflectionTestUtils.setField(producto, "idProducto", 1);
        String json = mapper.writeValueAsString(ProductoResponse.desde(producto));
        assertTrue(json.contains("\"precio\":4.80"));
        assertTrue(json.contains("\"descripcion\":\"Harina\""));
        assertFalse(json.contains("hibernateLazyInitializer"));
    }

    @Test void rechazaPaginacionFueraDeRango() {
        for (int[] valores : List.of(new int[]{0,10}, new int[]{1,0}, new int[]{1,101}, new int[]{1000001,10}))
            assertThrows(IllegalArgumentException.class, () -> service.listar("", null, null, valores[0], valores[1]));
        verifyNoInteractions(productos);
    }

    @Test void rechazaBusquedasExtensas() {
        assertThrows(IllegalArgumentException.class, () -> service.listar("x".repeat(151), null, null, 1, 10));
        verifyNoInteractions(productos);
    }

    @Test void ajustaPaginaFueraDeRangoManteniendoElConteo() {
        when(productos.buscar(eq(""), isNull(), isNull(), any(Pageable.class))).thenAnswer(invocation -> {
            Pageable pagina = invocation.getArgument(3);
            return new PageImpl<>(List.of(), pagina, 11);
        });
        var resultado = service.listar("", null, null, 1000000, 10);
        assertEquals(2, resultado.pagina());
        assertEquals(2, resultado.paginas());
        assertEquals(11, resultado.total());
        verify(productos, times(2)).buscar(eq(""), isNull(), isNull(), any(Pageable.class));
    }
}
