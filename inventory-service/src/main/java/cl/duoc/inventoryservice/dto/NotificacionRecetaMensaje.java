package cl.duoc.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload que inventory-service publica en la cola "notificacion-queue"
 * cuando termina de procesar la reserva de una receta. notification-service
 * lo consume para avisar al médico si su receta quedó RESERVADA o SIN_STOCK.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionRecetaMensaje {

    private Long recetaId;
    private String medicoUsername;
    private String pacienteNombre;
    private String estado;
    private String sucursal;
}
