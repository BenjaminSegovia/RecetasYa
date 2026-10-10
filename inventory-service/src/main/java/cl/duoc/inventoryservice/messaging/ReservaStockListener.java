package cl.duoc.inventoryservice.messaging;

import cl.duoc.inventoryservice.dto.NotificacionRecetaMensaje;
import cl.duoc.inventoryservice.dto.ReservaStockMensaje;
import cl.duoc.inventoryservice.service.RecetaStatusClient;
import cl.duoc.inventoryservice.service.RecetaStatusClient.RecetaStatusResponse;
import cl.duoc.inventoryservice.service.StockService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservaStockListener {
    
    private final StockService stockService;
    private final RecetaStatusClient recetaStatusClient;
    private final NotificacionRecetaPublisher notificacionRecetaPublisher;

    @SqsListener("${aws.sqs.queue-name}")
    public void escucharReservaStock(ReservaStockMensaje mensaje) {
        log.info("Mensaje recibido de SQS: receta {}", mensaje.getRecetaId());

        boolean hayStock = stockService.hayStockSuficiente(mensaje.getSucursal(), mensaje.getMedicamentos());

        String nuevoEstado = hayStock ? "RESERVADA" : "SIN_STOCK";

        if (hayStock) {
            stockService.reservarStock(mensaje.getSucursal(), mensaje.getMedicamentos());
        } else {
            log.warn("No hay stock suficiente para la receta {}", mensaje.getRecetaId());
        }

        RecetaStatusResponse receta = recetaStatusClient.actualizarEstado(mensaje.getRecetaId(), nuevoEstado);

        // Se publica el resultado en la cola de notificaciones para que
        // notification-service avise al médico (solo si receta-service
        // devolvió los datos necesarios).
        if (receta != null) {
            notificacionRecetaPublisher.publicar(NotificacionRecetaMensaje.builder()
                    .recetaId(receta.id())
                    .medicoUsername(receta.medicoUsername())
                    .pacienteNombre(receta.pacienteNombre())
                    .estado(nuevoEstado)
                    .sucursal(receta.sucursal())
                    .build());
        }
    }

}
