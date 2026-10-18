package cl.duoc.authservice.config;

import cl.duoc.authservice.model.Role;
import cl.duoc.authservice.model.Usuario;
import cl.duoc.authservice.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

/**
 * Datos de prueba para desarrollo. Se activa solo con el perfil "seed"
 * (SPRING_PROFILES_ACTIVE=seed, como está en el docker-compose.yml).
 *
 * Crea dos usuarios de ejemplo:
 *   - medico1  (MEDICO, con email para recibir notificaciones)
 *   - farma1   (FARMACEUTICO)
 * La contraseña de ambos viene de la variable de entorno SEED_PASSWORD,
 * para no dejarla hardcodeada en el código.
 *
 * Si los usuarios ya existen no se vuelven a crear, pero **si les falta el
 * email se completa**: usuarios creados antes de que existiera el campo
 * `email` quedaron con NULL y notification-service no podía avisarles
 * ("No hay email para el medico X"). Así el seed también arregla las bases
 * de datos existentes de cada quien, sin borrar datos con `down -v`.
 */
@Configuration
@Profile("seed")
@RequiredArgsConstructor
@Slf4j
public class SeedDataConfig {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${seed.password}")
    private String seedPassword;

    @Bean
    CommandLineRunner seedUsuarios() {
        return args -> {
            asegurarUsuario("medico1",
                    "Dr. Juan Pérez", "medico1@recetasya.cl", Role.MEDICO);
            asegurarUsuario("farma1",
                    "Ana Rojas", "farma1@recetasya.cl", Role.FARMACEUTICO);
            avisarUsuariosSinEmail();
        };
    }

    /**
     * Crea el usuario si no existe; si existe pero llegó sin email, se lo
     * completa (no toca la contraseña ni los demás datos).
     */
    private void asegurarUsuario(String username,
                                 String nombreCompleto, String email, Role role) {
        Optional<Usuario> existente = usuarioRepository.findByUsername(username);

        if (existente.isEmpty()) {
            usuarioRepository.save(Usuario.builder()
                    .username(username)
                    .password(passwordEncoder.encode(seedPassword))
                    .nombreCompleto(nombreCompleto)
                    .email(email)
                    .role(role)
                    .build());
            log.info("Usuario semilla '{}' creado (rol {})", username, role);
            return;
        }

        Usuario usuario = existente.get();
        if (estaVacio(usuario.getEmail())) {
            usuario.setEmail(email);
            usuarioRepository.save(usuario);
            log.info("Al usuario '{}' le faltaba el email; se completo con {}", username, email);
        } else {
            log.info("El usuario '{}' ya existe; no se vuelve a crear", username);
        }
    }

    /**
     * Un aviso en el arranque deja claro por qué ciertos médicos no reciben
     * correos: sin email, notification-service no tiene a quién escribirle.
     */
    private void avisarUsuariosSinEmail() {
        List<String> sinEmail = usuariosSinEmail();
        if (!sinEmail.isEmpty()) {
            log.warn("Usuarios sin email (no van a recibir notificaciones): {}", sinEmail);
        }
    }

    /** Usernames con email null o en blanco. */
    List<String> usuariosSinEmail() {
        return usuarioRepository.findAll().stream()
                .filter(u -> estaVacio(u.getEmail()))
                .map(Usuario::getUsername)
                .toList();
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
