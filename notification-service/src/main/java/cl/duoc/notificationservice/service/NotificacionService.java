package cl.duoc.notificationservice.service;

import cl.duoc.notificationservice.dto.NotificacionRecetaMensaje;
import cl.duoc.notificationservice.dto.UsuarioInternoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Notifica al médico por email cuando su receta cambia de estado
 * (RESERVADA o SIN_STOCK). Se dispara al consumir un mensaje de la
 * cola notificacion-queue.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionService {

    private final JavaMailSender mailSender;
    private final AuthClient authClient;

    @Value("${notification.from}")
    private String from;

    public void notificarMedico(NotificacionRecetaMensaje mensaje) {
        UsuarioInternoResponse usuario = authClient.obtenerUsuario(mensaje.getMedicoUsername());

        if (usuario == null || usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            log.warn("No hay email para el medico {}; no se puede notificar la receta {}",
                    mensaje.getMedicoUsername(), mensaje.getRecetaId());
            return;
        }

        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(from);
            email.setTo(usuario.getEmail());
            email.setSubject(asunto(mensaje));
            email.setText(cuerpo(usuario, mensaje));

            mailSender.send(email);
            log.info("Notificacion enviada a {} por la receta {} (estado {})",
                    usuario.getEmail(), mensaje.getRecetaId(), mensaje.getEstado());
        } catch (Exception e) {
            log.error("No se pudo enviar el email al medico {} por la receta {}: {}",
                    usuario.getEmail(), mensaje.getRecetaId(), e.getMessage());
        }
    }

    private String asunto(NotificacionRecetaMensaje mensaje) {
        if ("RESERVADA".equals(mensaje.getEstado())) {
            return "Su receta #" + mensaje.getRecetaId() + " fue RESERVADA";
        }
        return "Su receta #" + mensaje.getRecetaId() + " queda SIN_STOCK";
    }

    private String cuerpo(UsuarioInternoResponse usuario, NotificacionRecetaMensaje mensaje) {
        StringBuilder sb = new StringBuilder();
        sb.append("Estimado/a ").append(usuario.getNombreCompleto()).append(",\n\n");

        if ("RESERVADA".equals(mensaje.getEstado())) {
            sb.append("El stock de su receta #").append(mensaje.getRecetaId())
              .append(" fue reservado exitosamente en la sucursal ")
              .append(mensaje.getSucursal()).append(".\n")
              .append("El paciente ").append(mensaje.getPacienteNombre())
              .append(" ya puede pasar a retirarla.\n");
        } else {
            sb.append("No hay stock disponible para su receta #")
              .append(mensaje.getRecetaId())
              .append(" en la sucursal ").append(mensaje.getSucursal()).append(".\n")
              .append("El paciente ").append(mensaje.getPacienteNombre())
              .append(" no podrá retirarla por ahora.\n");
        }

        sb.append("\nSaludos cordiales,\nRecetasYa");
        return sb.toString();
    }

}
