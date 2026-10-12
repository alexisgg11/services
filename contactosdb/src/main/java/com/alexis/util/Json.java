package com.alexis.util;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/** Serializador pequeño de respuestas: no interpreta JSON recibido del cliente. */
public final class Json {
    private Json() {}
    public static String escribir(Object valor) {
        if (valor == null) return "null";
        if (valor instanceof String texto) return comillas(texto);
        if (valor instanceof Number || valor instanceof Boolean) return valor.toString();
        if (valor instanceof Collection<?> lista)
            return lista.stream().map(Json::escribir).collect(Collectors.joining(",", "[", "]"));
        if (valor instanceof Map<?, ?> mapa)
            return mapa.entrySet().stream().map(e -> comillas(e.getKey().toString()) + ":" + escribir(e.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
        if (valor.getClass().isRecord()) {
            var campos = new LinkedHashMap<String, Object>();
            for (var componente : valor.getClass().getRecordComponents()) {
                try { campos.put(componente.getName(), componente.getAccessor().invoke(valor)); }
                catch (IllegalAccessException | InvocationTargetException error) { throw new IllegalStateException(error); }
            }
            return escribir(campos);
        }
        throw new IllegalArgumentException("Tipo JSON no admitido.");
    }
    private static String comillas(String texto) {
        var salida = new StringBuilder("\"");
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"' -> salida.append("\\\"");
                case '\\' -> salida.append("\\\\");
                case '\n' -> salida.append("\\n");
                case '\r' -> salida.append("\\r");
                case '\t' -> salida.append("\\t");
                default -> { if (c < 32) salida.append(String.format("\\u%04x", (int)c)); else salida.append(c); }
            }
        }
        return salida.append('"').toString();
    }
}
