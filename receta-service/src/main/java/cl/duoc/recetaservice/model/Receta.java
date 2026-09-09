package cl.duoc.recetaservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "recetas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receta {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String medicoUsername;

    @Column(nullable = false)
    private String pacienteNombre;

    @Column(nullable = false)
    private String sucursal;

    @ElementCollection
    @CollectionTable(name = "receta_medicamentos", joinColumns = @JoinColumn(name = "receta_id"))
    private List<DetalleMedicamento> medicamentos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReceta estado;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

}
