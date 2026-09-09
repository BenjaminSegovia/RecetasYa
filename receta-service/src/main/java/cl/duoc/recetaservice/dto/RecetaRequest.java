package cl.duoc.recetaservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RecetaRequest {
    
    @NotBlank
    private String pacienteNombre;

    @NotBlank
    private String sucursal;

    @NotEmpty
    @Valid
    private List<DetalleMedicamentoDto> medicamentos;
}
