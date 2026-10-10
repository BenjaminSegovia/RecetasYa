package cl.duoc.notificationservice.service;

import cl.duoc.notificationservice.dto.UsuarioInternoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Cliente para llamar al endpoint interno de auth-service y obtener el
 * email del médico que debe ser notificado.
 */
@Service
@Slf4j
public class AuthClient {

    private final RestClient restClient;

    public AuthClient(@Value("${auth-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public UsuarioInternoResponse obtenerUsuario(String username) {
        try {
            return restClient.get()
                    .uri("/internal/usuarios/{username}", username)
                    .retrieve()
                    .body(UsuarioInternoResponse.class);
        } catch (Exception e) {
            log.error("No se pudo obtener el usuario {} de auth-service: {}",
                    username, e.getMessage());
            return null;
        }
    }

}
