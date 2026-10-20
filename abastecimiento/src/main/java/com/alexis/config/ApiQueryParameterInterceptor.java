package com.alexis.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.TreeSet;

public class ApiQueryParameterInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod metodo)) return true;

        var permitidos = new TreeSet<String>();
        for (var parametro : metodo.getMethodParameters()) {
            var anotacion = parametro.getParameterAnnotation(RequestParam.class);
            if (anotacion == null) continue;
            String nombre = !anotacion.name().isEmpty() ? anotacion.name() : anotacion.value();
            permitidos.add(nombre.isEmpty() ? parametro.getParameter().getName() : nombre);
        }

        var desconocidos = new TreeSet<>(request.getParameterMap().keySet());
        desconocidos.removeAll(permitidos);
        if (!desconocidos.isEmpty()) {
            throw new IllegalArgumentException("Solicitud inválida.");
        }
        return true;
    }
}
