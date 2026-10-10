package cl.duoc.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO interno que expone los datos básicos de un usuario (sin credenciales).
 * Lo consume notification-service para obtener el email del médico y poder
 * avisarle cuando su receta queda RESERVADA o SIN_STOCK.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioInternoResponse {

    private Long id;
    private String username;
    private String nombreCompleto;
    private String email;
}
