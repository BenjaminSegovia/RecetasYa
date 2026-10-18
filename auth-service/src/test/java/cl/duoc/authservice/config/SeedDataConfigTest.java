package cl.duoc.authservice.config;

import cl.duoc.authservice.model.Role;
import cl.duoc.authservice.model.Usuario;
import cl.duoc.authservice.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeedDataConfigTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SeedDataConfig seedDataConfig;

    @BeforeEach
    void setUp() {
        // El valor que en producción viene de SEED_PASSWORD.
        ReflectionTestUtils.setField(seedDataConfig, "seedPassword", "secret123");
    }

    private void correrSeed() throws Exception {
        seedDataConfig.seedUsuarios().run(new String[]{});
    }

    private Usuario usuario(String username, String email) {
        return Usuario.builder()
                .id(1L)
                .username(username)
                .password("$2a$hash")
                .nombreCompleto("Nombre")
                .email(email)
                .role(Role.MEDICO)
                .build();
    }

    @Test
    void creaElUsuarioSiNoExiste() throws Exception {
        when(usuarioRepository.findByUsername("medico1")).thenReturn(Optional.empty());
        when(usuarioRepository.findByUsername("farma1")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        correrSeed();

        verify(usuarioRepository).save(argThat(u ->
                "medico1".equals(u.getUsername())
                        && "$2a$hash".equals(u.getPassword())
                        && "medico1@recetasya.cl".equals(u.getEmail())
                        && u.getRole() == Role.MEDICO));
    }

    @Test
    void completaElEmailCuandoElUsuarioYaExisteSinEmail() throws Exception {
        when(usuarioRepository.findByUsername("medico1"))
                .thenReturn(Optional.of(usuario("medico1", null)));
        when(usuarioRepository.findByUsername("farma1"))
                .thenReturn(Optional.of(usuario("farma1", "farma1@recetasya.cl")));

        correrSeed();

        verify(usuarioRepository).save(argThat(u ->
                "medico1".equals(u.getUsername())
                        && "medico1@recetasya.cl".equals(u.getEmail())));
        // No se vuelve a crear ni se re-encripta la contraseña.
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void noTocaAlUsuarioSiYaTieneEmail() throws Exception {
        when(usuarioRepository.findByUsername("medico1"))
                .thenReturn(Optional.of(usuario("medico1", "otro@correo.cl")));
        when(usuarioRepository.findByUsername("farma1"))
                .thenReturn(Optional.of(usuario("farma1", "farma1@recetasya.cl")));

        correrSeed();

        verify(usuarioRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void listaSoloLosUsuariosConEmailVacio() {
        when(usuarioRepository.findAll()).thenReturn(List.of(
                usuario("medico1", null),
                usuario("farma1", "farma1@recetasya.cl"),
                usuario("dra.perez", "  "),
                usuario("otro", "otro@correo.cl")));

        List<String> sinEmail = seedDataConfig.usuariosSinEmail();

        assertThat(sinEmail).containsExactly("medico1", "dra.perez");
    }

    @Test
    void noAvisaCuandoTodosTienenEmail() {
        when(usuarioRepository.findAll()).thenReturn(List.of(
                usuario("farma1", "farma1@recetasya.cl")));

        assertThat(seedDataConfig.usuariosSinEmail()).isEmpty();
    }
}
