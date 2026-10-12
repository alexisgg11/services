package com.alexis.model;
import java.util.Map;
public record Resumen(long productos, Producto masCaro, Map<Integer, Long> productosPorProveedor) {}
