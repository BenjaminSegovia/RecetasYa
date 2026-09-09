package cl.duoc.recetaservice.dto;

import cl.duoc.recetaservice.model.EstadoReceta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

/**
 * DTO de respuesta para la entidad Receta.
 */

public class RecetaResponse {
    
    /**
     * Identificador único de la receta.
    */
    private Long id;

    /**
     * Nombre de usuario del médico que creó la receta.
     */
    private String medicoUsername;

    /**
     * Nombre del paciente para quien se creó la receta.
     */
    private String pacienteNombre;

    /**
     * Sucursal donde se creó la receta.
     */
    private String sucursal;

    /**
     * Lista de medicamentos incluidos en la receta.
     */
    private List<DetalleMedicamentoDto> medicamentos;

    /**
     * Estado de la receta.
     */ 
    private EstadoReceta estado;

    /**
     * Fecha y hora de creación de la receta.
     */
    private LocalDateTime fechaCreacion;
}
