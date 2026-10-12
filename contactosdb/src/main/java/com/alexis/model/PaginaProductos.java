package com.alexis.model;
import java.util.List;
public record PaginaProductos(List<Producto> items, long total, int pagina, int tamano, int paginas) {}
