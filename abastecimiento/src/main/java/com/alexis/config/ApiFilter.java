package com.alexis.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.*;
import java.nio.charset.StandardCharsets;

@Component
public class ApiFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
        if (!request.getRequestURI().startsWith("/api/")) { chain.doFilter(request,response); return; }
        if (request.getQueryString() != null && request.getQueryString().length() > 16384) {
            error(response,413,"Parámetros demasiado grandes."); return;
        }
        if (request.getParameterMap().values().stream().anyMatch(values -> values.length > 1)) {
            error(response,400,"No se permiten parámetros repetidos."); return;
        }
        if ("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod())) {
            byte[] body = request.getInputStream().readNBytes(16385);
            if (body.length > 16384) { error(response,413,"Solicitud demasiado grande."); return; }
            request = new BodyRequest(request,body);
        }
        chain.doFilter(request,response);
    }
    private void error(HttpServletResponse response,int status,String message) throws IOException {
        response.setStatus(status); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\",\"campos\":{}}");
    }
    private static class BodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;
        BodyRequest(HttpServletRequest request,byte[] body) { super(request); this.body=body; }
        @Override public ServletInputStream getInputStream() {
            var input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return input.read(); }
                @Override public boolean isFinished() { return input.available()==0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {
                    try { if (!isFinished()) listener.onDataAvailable(); if (isFinished()) listener.onAllDataRead(); }
                    catch(IOException error) { listener.onError(error); }
                }
            };
        }
        @Override public BufferedReader getReader() { return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8)); }
    }
}
