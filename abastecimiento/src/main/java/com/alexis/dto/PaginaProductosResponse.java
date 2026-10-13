package com.alexis.dto;
import com.alexis.model.PaginaProductos;
import java.util.List;
public record PaginaProductosResponse(List<ProductoResponse> items, long total, int pagina, int tamano, int paginas) {
    public static PaginaProductosResponse desde(PaginaProductos p) {
        return new PaginaProductosResponse(p.items().stream().map(ProductoResponse::desde).toList(), p.total(), p.pagina(), p.tamano(), p.paginas());
    }
}
