package cl.duoc.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta del endpoint interno GET /internal/usuarios/{username}
 * de auth-service (sin credenciales).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioInternoResponse {

    private Long id;
    private String username;
    private String nombreCompleto;
    private String email;
}
