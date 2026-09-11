package cl.duoc.authservice.service;

import cl.duoc.authservice.dto.AuthResponse;
import cl.duoc.authservice.dto.LoginRequest;
import cl.duoc.authservice.dto.RegisterRequest;
import cl.duoc.authservice.exception.InvalidCredentialsException;
import cl.duoc.authservice.exception.UsernameAlreadyExistsException;
import cl.duoc.authservice.model.Usuario;
import cl.duoc.authservice.repository.UsuarioRepository;
import cl.duoc.authservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    /**
     * Repositorio de usuarios para acceder a la base de datos.
     */
    private final UsuarioRepository usuarioRepository;
    /**
     * Codificador de contraseñas para almacenar contraseñas de forma segura.
     */
    private final PasswordEncoder passwordEncoder;
    /**
     * Servicio para generar y validar tokens JWT.
     */
    private final JwtService jwtService;

    /**
     * Registra un nuevo usuario en el sistema.
        * @param request La solicitud de registro que contiene el nombre de usuario, contraseña, nombre completo y rol.
        * @return Una respuesta de autenticación que contiene el token JWT, el nombre de usuario y el rol del usuario registrado.
        * @throws UsernameAlreadyExistsException Si el nombre de usuario ya está en uso.
     */
    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException(
                    "El username '" + request.getUsername() + "' ya está en uso");
        }

        Usuario usuario = Usuario.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nombreCompleto(request.getNombreCompleto())
                .role(request.getRole())
                .build();

        usuarioRepository.save(usuario);

        String token = jwtService.generateToken(usuario.getUsername(), usuario.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .username(usuario.getUsername())
                .role(usuario.getRole().name())
                .build();
    }

    /**
     * Inicia sesión en el sistema con un nombre de usuario y contraseña.
        * @param request La solicitud de inicio de sesión que contiene el nombre de usuario y la contraseña.
        * @return Una respuesta de autenticación que contiene el token JWT, el nombre de usuario y el rol del usuario autenticado.
        * @throws InvalidCredentialsException Si las credenciales proporcionadas son incorrectas.
     */
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Usuario o contraseña incorrectos"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new InvalidCredentialsException("Usuario o contraseña incorrectos");
        }

        String token = jwtService.generateToken(usuario.getUsername(), usuario.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .username(usuario.getUsername())
                .role(usuario.getRole().name())
                .build();
    }
}
