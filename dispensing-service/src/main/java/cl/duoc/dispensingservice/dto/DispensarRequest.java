package cl.duoc.dispensingservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DispensarRequest {
    
    @NotNull
    private Long recetaId;

}
