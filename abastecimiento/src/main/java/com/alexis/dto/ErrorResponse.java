package com.alexis.dto;
import java.util.Map;
public record ErrorResponse(String error, Map<String,String> campos) {
    public ErrorResponse(String error) { this(error, Map.of()); }
}
