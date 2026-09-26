package cl.duoc.dispensingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispensacionResponse {
    
    private Long id;
    private Long recetaId;
    private String farmaceuticoUsername;
    private String sucursal;
    private LocalDateTime fechaDispensacion;

}
