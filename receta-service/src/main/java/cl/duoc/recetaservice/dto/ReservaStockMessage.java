package cl.duoc.recetaservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload que receta-service publica en la cola SQS "reserva-stock-queue".
 * inventory-service (a través de la Lambda) lo consume para verificar
 * y reservar el stock correspondiente.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaStockMessage {
    
    /**
     * Identificador único de la receta.
     */
    private Long recetaId;
    
    /**
     * Nombre de usuario del médico que creó la receta.
     */
    private String sucursal;

    /**
     * Lista de medicamentos incluidos en la receta.
     */
    private List<DetalleMedicamentoDto> medicamentos;

}
