package cl.duoc.inventoryservice.messaging;

import cl.duoc.inventoryservice.dto.NotificacionRecetaMensaje;
import cl.duoc.inventoryservice.dto.ReservaStockMensaje;
import cl.duoc.inventoryservice.service.RecetaStatusClient;
import cl.duoc.inventoryservice.service.RecetaStatusClient.RecetaStatusResponse;
import cl.duoc.inventoryservice.service.StockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaStockListenerTest {

    @Mock
    private StockService stockService;

    @Mock
    private RecetaStatusClient recetaStatusClient;

    @Mock
    private NotificacionRecetaPublisher notificacionRecetaPublisher;

    @InjectMocks
    private ReservaStockListener listener;

    private ReservaStockMensaje mensaje() {
        ReservaStockMensaje mensaje = new ReservaStockMensaje();
        mensaje.setRecetaId(1L);
        mensaje.setSucursal("Santiago Centro");
        return mensaje;
    }

    private RecetaStatusResponse receta() {
        return new RecetaStatusResponse(1L, "medico1", "Juan Pérez", "Santiago Centro");
    }

    @Test
    void conStockReservaActualizaAReservadaYPublicaLaNotificacion() {
        when(stockService.hayStockSuficiente(anyString(), any())).thenReturn(true);
        when(recetaStatusClient.actualizarEstado(1L, "RESERVADA")).thenReturn(receta());

        listener.escucharReservaStock(mensaje());

        verify(stockService).reservarStock(anyString(), any());
        verify(recetaStatusClient).actualizarEstado(1L, "RESERVADA");

        ArgumentCaptor<NotificacionRecetaMensaje> captor =
                ArgumentCaptor.forClass(NotificacionRecetaMensaje.class);
        verify(notificacionRecetaPublisher).publicar(captor.capture());

        NotificacionRecetaMensaje publicado = captor.getValue();
        assertThat(publicado.getRecetaId()).isEqualTo(1L);
        assertThat(publicado.getEstado()).isEqualTo("RESERVADA");
        assertThat(publicado.getMedicoUsername()).isEqualTo("medico1");
        assertThat(publicado.getPacienteNombre()).isEqualTo("Juan Pérez");
        assertThat(publicado.getSucursal()).isEqualTo("Santiago Centro");
    }

    @Test
    void sinStockNoReservaCambiaASinStockYPublicaLaNotificacion() {
        when(stockService.hayStockSuficiente(anyString(), any())).thenReturn(false);
        when(recetaStatusClient.actualizarEstado(1L, "SIN_STOCK")).thenReturn(receta());

        listener.escucharReservaStock(mensaje());

        verify(stockService, never()).reservarStock(anyString(), any());
        verify(recetaStatusClient).actualizarEstado(1L, "SIN_STOCK");

        ArgumentCaptor<NotificacionRecetaMensaje> captor =
                ArgumentCaptor.forClass(NotificacionRecetaMensaje.class);
        verify(notificacionRecetaPublisher).publicar(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo("SIN_STOCK");
    }

    @Test
    void noPublicaNotificacionSiNoSePudoActualizarElEstado() {
        when(stockService.hayStockSuficiente(anyString(), any())).thenReturn(true);
        // receta-service no respondió (la llamada falló): client devuelve null.
        when(recetaStatusClient.actualizarEstado(anyLong(), anyString())).thenReturn(null);

        listener.escucharReservaStock(mensaje());

        verify(notificacionRecetaPublisher, never()).publicar(any());
    }
}
