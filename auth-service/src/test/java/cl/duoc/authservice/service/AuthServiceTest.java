package cl.duoc.authservice.service;

import cl.duoc.authservice.dto.AuthResponse;
import cl.duoc.authservice.dto.LoginRequest;
import cl.duoc.authservice.dto.RegisterRequest;
import cl.duoc.authservice.exception.InvalidCredentialsException;
import cl.duoc.authservice.exception.UsernameAlreadyExistsException;
import cl.duoc.authservice.model.Role;
import cl.duoc.authservice.model.Usuario;
import cl.duoc.authservice.repository.UsuarioRepository;
import cl.duoc.authservice.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("medico1");
        request.setPassword("secret123");
        request.setNombreCompleto("Dr. Prueba");
        request.setEmail("medico1@recetasya.cl");
        request.setRole(Role.MEDICO);
        return request;
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    @Test
    void registrarUsuarioNuevoDevuelveTokenYGuardaElHash() {
        when(usuarioRepository.existsByUsername("medico1")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateToken("medico1", "MEDICO")).thenReturn("token-seguro");

        AuthResponse response = authService.register(registerRequest());

        assertThat(response.getToken()).isEqualTo("token-seguro");
        assertThat(response.getUsername()).isEqualTo("medico1");
        assertThat(response.getRole()).isEqualTo("MEDICO");

        // Guarda el hash de la contraseña, nunca el texto plano,
        // y conserva el email para poder notificar al médico.
        verify(usuarioRepository).save(argThat(u ->
                "$2a$hash".equals(u.getPassword())
                        && "medico1@recetasya.cl".equals(u.getEmail())
                        && u.getRole() == Role.MEDICO));
    }

    @Test
    void registrarUsuarioConUsernameDuplicadoLanzaError() {
        when(usuarioRepository.existsByUsername("medico1")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(usuarioRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void loginCorrectoDevuelveToken() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .username("medico1")
                .password("$2a$hash")
                .nombreCompleto("Dr. Prueba")
                .role(Role.MEDICO)
                .build();

        when(usuarioRepository.findByUsername("medico1")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("secret123", "$2a$hash")).thenReturn(true);
        when(jwtService.generateToken("medico1", "MEDICO")).thenReturn("token-seguro");

        AuthResponse response = authService.login(loginRequest("medico1", "secret123"));

        assertThat(response.getToken()).isEqualTo("token-seguro");
        assertThat(response.getUsername()).isEqualTo("medico1");
    }

    @Test
    void loginConPasswordIncorrectaLanzaError() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .username("medico1")
                .password("$2a$hash")
                .nombreCompleto("Dr. Prueba")
                .role(Role.MEDICO)
                .build();

        when(usuarioRepository.findByUsername("medico1")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("otra-password", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest("medico1", "otra-password")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void loginConUsuarioInexistenteLanzaError() {
        when(usuarioRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest("fantasma", "secret123")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateToken(any(), any());
    }
}
