package io.github.raulramalf.browseme.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Cabeceras de seguridad. La API (JSON) recibe la CSP más estricta. Las páginas
 * del frontend solo las sirve el backend en la app de escritorio (perfil
 * {@code local}); en Docker las sirve nginx con sus propias cabeceras. Su CSP
 * es la misma que la de nginx.
 */
public class SecurityHeadersFilter extends OncePerRequestFilter {

    static final String API_CSP = "default-src 'none'; frame-ancestors 'none'";
    static final String PAGE_CSP = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
            + "connect-src 'self'; font-src 'self'; object-src 'none'; base-uri 'none'; form-action 'self'; "
            + "frame-ancestors 'none'";

    private final boolean hsts;

    public SecurityHeadersFilter(boolean hsts) {
        this.hsts = hsts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean page = !path.startsWith("/api/") && !path.startsWith("/actuator");
        response.setHeader("Content-Security-Policy", page ? PAGE_CSP : API_CSP);
        if (page) {
            // Sin esto el navegador puede seguir mostrando la pantalla de una versión anterior desde su
            // caché aunque el programa sea nuevo. Los archivos con hash en el nombre sí son inmutables.
            response.setHeader("Cache-Control", path.startsWith("/assets/")
                    ? "public, max-age=31536000, immutable" : "no-cache");
        }
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("X-Frame-Options", "DENY");
        if (hsts) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        chain.doFilter(request, response);
    }
}
