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
     *
     * @return la receta actualizada, o null si la llamada falla.
     */
    public RecetaStatusResponse actualizarEstado(Long recetaId, String nuevoEstado) {
        try {
            RecetaStatusResponse response = restClient.patch()
                    .uri("/recetas/{id}/estado", recetaId)
                    .body(Map.of("estado", nuevoEstado))
                    .retrieve()
                    .body(RecetaStatusResponse.class);
            log.info("Receta {} actualizada a estado {}", recetaId, nuevoEstado);
            return response;
        } catch (Exception e) {
            log.error("No se pudo actualizar el estado de la receta {}: {}", recetaId, e.getMessage());
            return null;
        }
    }

    /**
     * Respuesta mínima de receta-service tras cambiar el estado: solo lo
     * necesario para notificar al médico (username, paciente y sucursal).
     */
    public record RecetaStatusResponse(Long id, String medicoUsername,
                                       String pacienteNombre, String sucursal) {
    }

}
