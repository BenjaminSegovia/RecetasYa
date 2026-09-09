package cl.duoc.recetaservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleMedicamentoDto {
    
    @NotBlank
    private String nombreMedicamento;

    @Min(1)
    private int cantidad;
}
