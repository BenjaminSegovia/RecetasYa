package cl.duoc.notificationservice.messaging;

import cl.duoc.notificationservice.dto.NotificacionRecetaMensaje;
import cl.duoc.notificationservice.service.NotificacionService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Consume la cola notificacion-queue: cada mensaje representa el resultado
 * del procesamiento de una receta (RESERVADA o SIN_STOCK) y dispara la
 * notificación por email al médico.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacionRecetaListener {

    private final NotificacionService notificacionService;

    @SqsListener("${aws.sqs.notificacion-queue-name}")
    public void escucharNotificacionReceta(NotificacionRecetaMensaje mensaje) {
        log.info("Mensaje de notificacion recibido: receta {} -> estado {}",
                mensaje.getRecetaId(), mensaje.getEstado());
        notificacionService.notificarMedico(mensaje);
    }

}
