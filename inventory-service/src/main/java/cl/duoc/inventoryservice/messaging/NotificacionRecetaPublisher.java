package cl.duoc.inventoryservice.messaging;

import cl.duoc.inventoryservice.dto.NotificacionRecetaMensaje;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Publica el resultado del procesamiento de una receta en la cola
 * "notificacion-queue", para que notification-service avise al médico.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionRecetaPublisher {

    private final SqsTemplate sqsTemplate;

    @Value("${aws.sqs.notificacion-queue-name}")
    private String notificacionQueueName;

    public void publicar(NotificacionRecetaMensaje mensaje) {
        log.info("Publicando notificacion de receta {} (estado {}) para el medico {}",
                mensaje.getRecetaId(), mensaje.getEstado(), mensaje.getMedicoUsername());
        sqsTemplate.send(to -> to.queue(notificacionQueueName).payload(mensaje));
    }
}
