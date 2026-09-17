package cl.duoc.recetaservice.dto;

import cl.duoc.recetaservice.model.EstadoReceta;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ActualizarEstadoRequest {
    
    @NotNull
    private EstadoReceta estado;

}
