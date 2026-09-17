package cl.duoc.inventoryservice.messaging;

import cl.duoc.inventoryservice.dto.ReservaStockMensaje;
import cl.duoc.inventoryservice.service.RecetaStatusClient;
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

    @SqsListener("${aws.sqs.queue-name}")
    public void escucharReservaStock(ReservaStockMensaje mensaje) {
        log.info("Mensaje recibido de SQS: receta {}", mensaje.getRecetaId());

        boolean hayStock = stockService.hayStockSuficiente(mensaje.getSucursal(), mensaje.getMedicamentos());

        if (hayStock) {
            stockService.reservarStock(mensaje.getSucursal(), mensaje.getMedicamentos());
            recetaStatusClient.actualizarEstado(mensaje.getRecetaId(), "RESERVADA");
        } else {
            log.warn("No hay stock suficiente para la receta {}", mensaje.getRecetaId());
            recetaStatusClient.actualizarEstado(mensaje.getRecetaId(), "SIN_STOCK");
        }
    }

}
