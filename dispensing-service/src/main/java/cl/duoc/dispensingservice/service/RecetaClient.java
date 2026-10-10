package cl.duoc.dispensingservice.service;

import cl.duoc.dispensingservice.dto.RecetaInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class RecetaClient {
    
    private final RestClient restClient;
    private final String internalApiKey;

    public RecetaClient(@Value("${receta-service.base-url}") String baseUrl,
                        @Value("${internal.api-key}") String internalApiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.internalApiKey = internalApiKey;
    }

    /**
     * GET /recetas/{id}: usa el JWT del farmacéutico que está dispensando
     * (receta-service exige rol MEDICO o FARMACEUTICO).
     */
    public RecetaInfo obtenerReceta(Long recetaId, String token) {
        return restClient.get()
                .uri("/recetas/{id}", recetaId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(RecetaInfo.class);
    }

    /**
     * PATCH /recetas/{id}/estado: endpoint interno protegido con
     * X-Internal-Api-Key (no con el JWT del usuario).
     */
    public void marcarComoDispensada(Long recetaId) {
        restClient.patch()
                .uri("/recetas/{id}/estado", recetaId)
                .header("X-Internal-Api-Key", internalApiKey)
                .body(java.util.Map.of("estado", "DISPENSADA"))
                .retrieve()
                .toBodilessEntity();
    }

}
