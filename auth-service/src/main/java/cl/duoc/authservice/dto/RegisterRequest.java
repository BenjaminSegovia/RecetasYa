package cl.duoc.authservice.dto;

import cl.duoc.authservice.model.Role;
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

    @NotNull
    private Role role;
}
