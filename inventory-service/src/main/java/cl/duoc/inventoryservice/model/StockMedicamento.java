package cl.duoc.inventoryservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock_medicamento", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"nombre_medicamento", "sucursal"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMedicamento {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_medicamento", nullable = false)
    private String nombreMedicamento;

    @Column(nullable = false)
    private String sucursal;

    @Column(nullable = false)
    private int cantidadDisponible;


}
