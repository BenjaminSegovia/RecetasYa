package cl.duoc.recetaservice.service;

import cl.duoc.recetaservice.dto.ReservaStockMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservaStockPublisher {
    
    private final SqsTemplate sqsTemplate;

    @Value("${aws.sqs.queue-name}")
    private String queueName;

    public void publicar(ReservaStockMessage mensaje) {
        log.info("Publicando solicitud de reserva de stock para receta {}", mensaje.getRecetaId());
        sqsTemplate.send(to -> to.queue(queueName).payload(mensaje));
    }
}
