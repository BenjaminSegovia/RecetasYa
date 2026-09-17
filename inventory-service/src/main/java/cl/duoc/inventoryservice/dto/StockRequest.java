package cl.duoc.inventoryservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StockRequest {
    
    @NotBlank
    private String nombreMedicamento;

    @NotBlank
    private String sucursal;

    @Min(0)
    private int cantidadDisponible;

}
