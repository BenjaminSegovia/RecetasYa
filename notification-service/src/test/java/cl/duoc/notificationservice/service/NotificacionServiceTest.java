package cl.duoc.notificationservice.service;

import cl.duoc.notificationservice.dto.NotificacionRecetaMensaje;
import cl.duoc.notificationservice.dto.UsuarioInternoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private AuthClient authClient;

    @InjectMocks
    private NotificacionService notificacionService;

    private NotificacionRecetaMensaje mensaje(String estado) {
        NotificacionRecetaMensaje mensaje = new NotificacionRecetaMensaje();
        mensaje.setRecetaId(1L);
        mensaje.setMedicoUsername("medico1");
        mensaje.setPacienteNombre("Juan Pérez");
        mensaje.setEstado(estado);
        mensaje.setSucursal("Santiago Centro");
        return mensaje;
    }

    private UsuarioInternoResponse medico() {
        return new UsuarioInternoResponse(1L, "medico1", "Dr. Prueba", "medico1@recetasya.cl");
    }

    @Test
    void enviaEmailAlMedicoCuandoLaRecetaQuedaReservada() {
        when(authClient.obtenerUsuario("medico1")).thenReturn(medico());

        notificacionService.notificarMedico(mensaje("RESERVADA"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage email = captor.getValue();
        assertThat(email.getTo()).containsExactly("medico1@recetasya.cl");
        assertThat(email.getSubject()).contains("RESERVADA");
        assertThat(email.getText()).contains("Juan Pérez");
        assertThat(email.getText()).contains("Santiago Centro");
    }

    @Test
    void enviaEmailDeSinStockCuandoNoHayStock() {
        when(authClient.obtenerUsuario("medico1")).thenReturn(medico());

        notificacionService.notificarMedico(mensaje("SIN_STOCK"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getSubject()).contains("SIN_STOCK");
    }

    @Test
    void noEnviaEmailSiElMedicoNoTieneEmail() {
        UsuarioInternoResponse sinEmail = new UsuarioInternoResponse(1L, "medico1", "Dr. Prueba", null);
        when(authClient.obtenerUsuario("medico1")).thenReturn(sinEmail);

        notificacionService.notificarMedico(mensaje("RESERVADA"));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void noEnviaEmailSiElMedicoNoExisteEnAuth() {
        when(authClient.obtenerUsuario("medico1")).thenReturn(null);

        notificacionService.notificarMedico(mensaje("RESERVADA"));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
