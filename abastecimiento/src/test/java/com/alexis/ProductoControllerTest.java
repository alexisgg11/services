package com.alexis;

import com.alexis.config.ApiFilter;
import com.alexis.config.ApiQueryParameterInterceptor;
import com.alexis.controller.*;
import com.alexis.dto.ProductoRequest;
import com.alexis.model.*;
import com.alexis.service.ProductoService;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import java.math.BigDecimal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductoControllerTest {
    private final StubService service = new StubService();
    private LocalValidatorFactoryBean validator;
    private MockMvc mvc;
    private static final String VALID = """
        {"descripcion":"Harina","precio":"4.80","idTipo":"1","idProveedor":"1","idUnidad":"1"}
        """;
    private static class StubService extends ProductoService {
        boolean saved;
        boolean listed;
        List<Object> filtros;
        StubService() { super(null, null, null, null); }
        @Override public int guardar(Integer id, ProductoRequest data) {
            if (data.idProveedor().equals("9999")) throw new DataIntegrityViolationException("privado");
            saved=true; return id == null ? 51 : id;
        }
        @Override public Producto masCaro() {
            var producto = new Producto("Harina", new BigDecimal("4.80"), new TipoProducto(1, "Tipo"),
                new Proveedor(1, "20000000001", "Proveedor"), new UnidadMedida(1, "Kilogramo", "kg"));
            ReflectionTestUtils.setField(producto, "idProducto", 1);
            return producto;
        }
        @Override public PaginaProductos listar(String b,Integer p,Integer t,int page,int size) {
            listed=true; filtros=Arrays.asList(b,p,t,page,size);
            return new PaginaProductos(List.of(masCaro()),1,page,size,1);
        }
    }
    @BeforeEach void setup() {
        validator = new LocalValidatorFactoryBean(); validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ProductoController(service)).setControllerAdvice(new ApiExceptionHandler())
            .setValidator(validator).addFilters(new ApiFilter())
            .addMappedInterceptors(new String[]{"/api/**"}, new ApiQueryParameterInterceptor()).build();
    }
    @AfterEach void close() { validator.close(); }
    @Test void registraJsonYDevuelveCreatedConLocation() throws Exception {
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(VALID))
            .andExpect(status().isCreated()).andExpect(header().string("Location","/api/productos/51"))
            .andExpect(jsonPath("$.idProducto").value(51));
        assertTrue(service.saved);
    }
    @Test void dtoRechazaCamposFaltantesSinGuardar() throws Exception {
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.descripcion").exists());
        assertFalse(service.saved);
    }
    @Test void validaFormatoTambienAlEditar() throws Exception {
        mvc.perform(put("/api/productos/1").contentType(MediaType.APPLICATION_JSON).content(VALID.replace("4.80","1e2")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.precio").exists());
        assertFalse(service.saved);
    }
    @Test void respuestasPaginadasYServiciosUsanDto() throws Exception {
        mvc.perform(get("/api/productos")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].descripcion").value("Harina"));
        mvc.perform(get("/api/servicios/producto-mas-caro")).andExpect(status().isOk()).andExpect(jsonPath("$.precio").value(4.8));
    }
    @Test void conservaRestriccionesDeSolicitudYFormato() throws Exception {
        mvc.perform(get("/api/productos?buscar=Harina&buscar=Sal")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/productos").contentType(MediaType.TEXT_PLAIN).content(VALID))
            .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content("x".repeat(16385)))
            .andExpect(status().is(413));
    }
    @Test void restriccionSqlNoExponeDetallesInternos() throws Exception {
        String result = mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(VALID.replace("\"idProveedor\":\"1\"","\"idProveedor\":\"9999\"")))
            .andExpect(status().isConflict()).andReturn().getResponse().getContentAsString();
        assertFalse(result.contains("privado"));
    }
    @Test void parametrosEnLaUrlNoEjecutanUnaInsercion() throws Exception {
        mvc.perform(get("/api/productos").param("descripcion", "Prueba GET").param("precio", "4.80"))
            .andExpect(status().isBadRequest());
        assertFalse(service.saved);
        assertFalse(service.listed);
    }
    @Test void rechazaParametrosDesconocidosAntesDeConsultar() throws Exception {
        for (String nombre : List.of("paginas", "página", "tamanoo")) {
            mvc.perform(get("/api/productos").param(nombre,"6"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Solicitud inválida."));
        }
        assertFalse(service.listed);
    }
    @Test void conservaParametrosValidosYValoresPorDefecto() throws Exception {
        mvc.perform(get("/api/productos")).andExpect(status().isOk());
        assertEquals(Arrays.asList("",null,null,1,10),service.filtros);
        mvc.perform(get("/api/productos").param("buscar","Harina").param("proveedor","2")
            .param("tipo","3").param("pagina","2").param("tamano","5"))
            .andExpect(status().isOk());
        assertEquals(List.of("Harina",2,3,2,5),service.filtros);
    }
    @Test void parametrosPermitidosDependenDeCadaEndpoint() throws Exception {
        mvc.perform(get("/api/servicios/por-tipo").param("id","3").param("pagina","2").param("tamano","5"))
            .andExpect(status().isOk());
        assertEquals(Arrays.asList("",null,3,2,5),service.filtros);
        mvc.perform(get("/api/servicios/por-tipo").param("id","3").param("buscar","Harina"))
            .andExpect(status().isBadRequest());
        for (String ruta : List.of("/api/catalogos", "/api/resumen", "/api/productos/1", "/api/servicios/producto-mas-caro")) {
            mvc.perform(get(ruta).param("pagina","1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Solicitud inválida."));
        }
    }
    @Test void escriturasRechazanParametrosEnLaUrlSinGuardar() throws Exception {
        mvc.perform(post("/api/productos").param("extra","1").contentType(MediaType.APPLICATION_JSON).content(VALID))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/productos/1").param("precio","5.00").contentType(MediaType.APPLICATION_JSON).content(VALID))
            .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/productos/1").param("extra","1"))
            .andExpect(status().isBadRequest());
        assertFalse(service.saved);
    }
}
