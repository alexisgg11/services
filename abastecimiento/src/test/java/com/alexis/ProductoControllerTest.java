package com.alexis;

import com.alexis.config.ApiFilter;
import com.alexis.controller.*;
import com.alexis.model.*;
import com.alexis.service.ProductoService;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import java.math.BigDecimal;
import java.sql.SQLException;
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
        StubService() { super(null); }
        @Override public int guardar(Integer id, Map<String,String> data) throws SQLException {
            if (data.get("idProveedor").equals("9999")) throw new SQLException("privado", "23503");
            saved=true; return id == null ? 51 : id;
        }
        @Override public Producto masCaro() { return new Producto(1,"Harina",new BigDecimal("4.80"),1,"Tipo",1,"Proveedor",1,"Kilogramo","kg"); }
        @Override public PaginaProductos listar(String b,Integer p,Integer t,int page,int size) { return new PaginaProductos(List.of(masCaro()),1,page,size,1); }
    }
    @BeforeEach void setup() {
        validator = new LocalValidatorFactoryBean(); validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ProductoController(service)).setControllerAdvice(new ApiExceptionHandler())
            .setValidator(validator).addFilters(new ApiFilter()).build();
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
            .andExpect(status().isPayloadTooLarge());
    }
    @Test void restriccionSqlNoExponeDetallesInternos() throws Exception {
        String result = mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(VALID.replace("\"idProveedor\":\"1\"","\"idProveedor\":\"9999\"")))
            .andExpect(status().isConflict()).andReturn().getResponse().getContentAsString();
        assertFalse(result.contains("privado"));
    }
}
