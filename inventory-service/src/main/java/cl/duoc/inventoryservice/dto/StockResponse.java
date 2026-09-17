package cl.duoc.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockResponse {
    
    private Long id;

    private String nombreMedicamento;

    private String sucursal;
    
    private int cantidadDisponible;

}