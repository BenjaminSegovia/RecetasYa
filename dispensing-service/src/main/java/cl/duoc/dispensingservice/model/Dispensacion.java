package cl.duoc.dispensingservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dispensaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispensacion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long recetaId;

    @Column(nullable = false)
    private String farmaceuticoUsername;

    @Column(nullable = false)
    private String sucursal;

    @Column(nullable = false)
    private LocalDateTime fechaDispensacion;

}
