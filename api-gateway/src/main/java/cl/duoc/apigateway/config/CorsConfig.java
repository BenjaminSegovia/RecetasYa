package cl.duoc.apigateway.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS en el punto de entrada.
 *
 * El frontend corre en otro origen que la API (Live Server en
 * http://localhost:5501 y el gateway en http://localhost:8080), así que el
 * navegador exige CORS: sin este filtro, cada llamada falla con
 * "Failed to fetch" por bloqueo de mismo origen.
 *
 * Se resuelve aquí y no en cada microservicio porque el gateway es el único
 * punto por donde pasa el navegador. Además Spring Cloud Gateway (variante
 * webmvc) no trae soporte propio de CORS y el preflight OPTIONS se responde
 * en el gateway sin llegar a los servicios de atrás.
 *
 * Los orígenes se configuran con FRONTEND_ORIGINS (por defecto el puerto del
 * Live Server).
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter(
            @Value("${frontend.origins:http://localhost:5501,http://127.0.0.1:5501}")
            String frontendOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(frontendOrigins.split("\\s*,\\s*")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Authorization (JWT), Content-Type y X-Internal-Api-Key.
        config.setAllowedHeaders(List.of("*"));
        // Guarda la respuesta del preflight por 1 hora para no repetirlo.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
