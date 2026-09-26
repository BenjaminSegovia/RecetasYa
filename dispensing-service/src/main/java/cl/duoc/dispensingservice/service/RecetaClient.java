package cl.duoc.dispensingservice.service;

import cl.duoc.dispensingservice.dto.RecetaInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class RecetaClient {
    
    private final RestClient restClient;

    public RecetaClient(@Value("${receta-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public RecetaInfo obtenerReceta(Long recetaId, String token) {
        return restClient.get()
                .uri("/recetas/{id}", recetaId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(RecetaInfo.class);
    }

    public void marcarComoDispensada(Long recetaId) {
        restClient.patch()
                .uri("/recetas/{id}/estado", recetaId)
                .body(java.util.Map.of("estado", "DISPENSADA"))
                .retrieve()
                .toBodilessEntity();
    }

}
