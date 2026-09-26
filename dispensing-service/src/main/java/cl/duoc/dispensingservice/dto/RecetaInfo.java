package cl.duoc.dispensingservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RecetaInfo {
    
    private Long id;
    private String sucursal;
    private String estado;

}
