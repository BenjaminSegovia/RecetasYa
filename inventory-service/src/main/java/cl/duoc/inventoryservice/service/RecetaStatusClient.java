package cl.duoc.inventoryservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@Slf4j
public class RecetaStatusClient {
    
    private final RestClient restClient;

    public RecetaStatusClient(@Value("${receta-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Le avisa a receta-service que la receta con este id cambió de estado
     * (ej: a RESERVADA cuando se confirmó el stock).
     */
    public void actualizarEstado(Long recetaId, String nuevoEstado) {
        try {
            restClient.patch()
                    .uri("/recetas/{id}/estado", recetaId)
                    .body(Map.of("estado", nuevoEstado))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Receta {} actualizada a estado {}", recetaId, nuevoEstado);
        } catch (Exception e) {
            log.error("No se pudo actualizar el estado de la receta {}: {}", recetaId, e.getMessage());
        }
    }

}
