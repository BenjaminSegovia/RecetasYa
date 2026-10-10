package cl.duoc.notificationservice.messaging;

import cl.duoc.notificationservice.dto.NotificacionRecetaMensaje;
import cl.duoc.notificationservice.service.NotificacionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificacionRecetaListenerTest {

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private NotificacionRecetaListener listener;

    @Test
    void alRecibirElMensajeDelegaEnElServicioDeNotificacion() {
        NotificacionRecetaMensaje mensaje = new NotificacionRecetaMensaje();
        mensaje.setRecetaId(1L);
        mensaje.setMedicoUsername("medico1");
        mensaje.setEstado("RESERVADA");

        listener.escucharNotificacionReceta(mensaje);

        // El listener solo recibe el mensaje de SQS y lo pasa al servicio.
        verify(notificacionService).notificarMedico(mensaje);
    }
}
