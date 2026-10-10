package cl.duoc.recetaservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protege los endpoints internos (hoy PATCH /recetas/{id}/estado) exigiendo
 * la cabecera X-Internal-Api-Key con la clave compartida entre servicios.
 * Sin esta clave, cualquier persona podría cambiar el estado de una receta.
 */
@RequiredArgsConstructor
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Api-Key";

    private final String apiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (esRutaInterna(request) && !apiKeyValida(request.getHeader(HEADER))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Falta o es incorrecta la cabecera X-Internal-Api-Key\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Ruta interna: PATCH /recetas/{id}/estado (la llaman inventory-service
     * y dispensing-service, nunca los clientes).
     */
    private boolean esRutaInterna(HttpServletRequest request) {
        return "PATCH".equals(request.getMethod())
                && request.getRequestURI().matches("/recetas/\\d+/estado");
    }

    /**
     * Comparación en tiempo constante para no filtrar la clave por timing.
     */
    private boolean apiKeyValida(String header) {
        if (header == null) {
            return false;
        }
        return MessageDigest.isEqual(
                header.getBytes(StandardCharsets.UTF_8),
                apiKey.getBytes(StandardCharsets.UTF_8));
    }
}
