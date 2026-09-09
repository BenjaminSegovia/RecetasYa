package cl.duoc.recetaservice.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleMedicamento {
    private String nombreMedicamento;
    private int cantidad;
}
