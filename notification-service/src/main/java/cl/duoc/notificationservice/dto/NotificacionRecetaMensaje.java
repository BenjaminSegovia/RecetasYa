package cl.duoc.notificationservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload que inventory-service publica en la cola "notificacion-queue"
 * cuando termina de procesar la reserva de una receta.
 */
@Data
@NoArgsConstructor
public class NotificacionRecetaMensaje {

    private Long recetaId;
    private String medicoUsername;
    private String pacienteNombre;
    private String estado;
    private String sucursal;
}
