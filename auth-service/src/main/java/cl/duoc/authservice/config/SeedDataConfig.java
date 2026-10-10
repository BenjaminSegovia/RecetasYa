package cl.duoc.authservice.config;

import cl.duoc.authservice.model.Role;
import cl.duoc.authservice.model.Usuario;
import cl.duoc.authservice.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Datos de prueba para desarrollo. Se activa solo con el perfil "seed"
 * (SPRING_PROFILES_ACTIVE=seed, como está en el docker-compose.yml).
 *
 * Crea dos usuarios de ejemplo si no existen todavía:
 *   - medico1 / secret123  (MEDICO, con email para recibir notificaciones)
 *   - farma1  / secret123  (FARMACEUTICO)
 *
 * Si los usuarios ya existen (por ejemplo, se reinicia el contenedor),
 * no toca nada: sirve tanto para una BD recién creada como para una
 * que ya venía con datos.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SeedDataConfig {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Profile("seed")
    CommandLineRunner seedUsuarios() {
        return args -> {
            crearSiNoExiste("medico1", "secret123",
                    "Dr. Juan Pérez", "medico1@recetasya.cl", Role.MEDICO);
            crearSiNoExiste("farma1", "secret123",
                    "Ana Rojas", "farma1@recetasya.cl", Role.FARMACEUTICO);
        };
    }

    private void crearSiNoExiste(String username, String password,
                                 String nombreCompleto, String email, Role role) {
        if (usuarioRepository.existsByUsername(username)) {
            log.info("El usuario '{}' ya existe; no se vuelve a crear", username);
            return;
        }

        usuarioRepository.save(Usuario.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .nombreCompleto(nombreCompleto)
                .email(email)
                .role(role)
                .build());

        log.info("Usuario semilla '{}' creado (rol {})", username, role);
    }
}
