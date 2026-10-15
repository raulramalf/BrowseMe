package io.github.raulramalf.browseme.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Cabeceras de seguridad para las respuestas de la API (solo JSON). El frontend
 * las suyas las pone nginx.
 */
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private final boolean hsts;

    public SecurityHeadersFilter(boolean hsts) {
        this.hsts = hsts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("X-Frame-Options", "DENY");
        if (hsts) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        chain.doFilter(request, response);
    }
}
