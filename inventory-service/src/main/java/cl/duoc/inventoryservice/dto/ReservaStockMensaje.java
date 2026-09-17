package cl.duoc.inventoryservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ReservaStockMensaje {
    
    private Long recetaId;
    private String sucursal;
    private List<DetalleMedicamentoMensaje> medicamentos;

}
