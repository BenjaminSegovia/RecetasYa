package cl.duoc.authservice.dto;

import cl.duoc.authservice.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequest {
    
    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String nombreCompleto;

    /**
     * Email del médico. Opcional, pero de enviarse debe ser válido:
     * notification-service lo usa para avisarle del estado de sus recetas.
     */
    @Email
    private String email;

    @NotNull
    private Role role;
}
