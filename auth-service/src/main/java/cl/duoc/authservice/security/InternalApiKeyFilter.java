package cl.duoc.authservice.security;

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
 * Protege los endpoints internos (hoy GET /internal/usuarios/{username})
 * exigiendo la cabecera X-Internal-Api-Key con la clave compartida entre
 * servicios. La consume notification-service para obtener el email del médico.
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

    /** Ruta interna: GET /internal/usuarios/{username}. */
    private boolean esRutaInterna(HttpServletRequest request) {
        return "GET".equals(request.getMethod())
                && request.getRequestURI().matches("/internal/usuarios/[^/]+");
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
