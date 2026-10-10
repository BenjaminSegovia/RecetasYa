package cl.duoc.authservice.controller;

import cl.duoc.authservice.dto.UsuarioInternoResponse;
import cl.duoc.authservice.exception.UsernameNotFoundException;
import cl.duoc.authservice.model.Usuario;
import cl.duoc.authservice.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints internos para que otros servicios consulten datos de usuarios.
 * Solo se expone lo mínimo (sin password): aquí notification-service busca
 * el email del médico para notificarle el estado de su receta.
 */
@RestController
@RequestMapping("/internal/usuarios")
@RequiredArgsConstructor
public class UsuarioInternoController {

    private final UsuarioRepository usuarioRepository;

    @GetMapping("/{username}")
    public ResponseEntity<UsuarioInternoResponse> obtenerPorUsername(@PathVariable String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No existe el usuario '" + username + "'"));

        return ResponseEntity.ok(UsuarioInternoResponse.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .nombreCompleto(usuario.getNombreCompleto())
                .email(usuario.getEmail())
                .build());
    }
}
