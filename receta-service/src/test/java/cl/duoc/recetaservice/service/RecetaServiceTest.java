package cl.duoc.recetaservice.service;

import cl.duoc.recetaservice.dto.DetalleMedicamentoDto;
import cl.duoc.recetaservice.dto.RecetaRequest;
import cl.duoc.recetaservice.dto.RecetaResponse;
import cl.duoc.recetaservice.dto.RecetaUpdateRequest;
import cl.duoc.recetaservice.dto.ReservaStockMessage;
import cl.duoc.recetaservice.exception.RecetaNoModificableException;
import cl.duoc.recetaservice.exception.RecetaNotFoundException;
import cl.duoc.recetaservice.model.DetalleMedicamento;
import cl.duoc.recetaservice.model.EstadoReceta;
import cl.duoc.recetaservice.model.Receta;
import cl.duoc.recetaservice.repository.RecetaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecetaServiceTest {

    @Mock
    private RecetaRepository recetaRepository;

    @Mock
    private ReservaStockPublisher reservaStockPublisher;

    @InjectMocks
    private RecetaService recetaService;

    @AfterEach
    void limpiarSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /** Simula al médico autenticado (el subject del JWT es el username). */
    private void autenticarComoMedico(String username) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(username)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, null, List.of()));
    }

    private RecetaRequest recetaRequest() {
        RecetaRequest request = new RecetaRequest();
        request.setPacienteNombre("Juan Pérez");
        request.setSucursal("Santiago Centro");
        request.setMedicamentos(List.of(new DetalleMedicamentoDto("Ibuprofeno 400mg", 2)));
        return request;
    }

    private Receta recetaGuardada(EstadoReceta estado) {
        return Receta.builder()
                .id(1L)
                .medicoUsername("medico1")
                .pacienteNombre("Juan Pérez")
                .sucursal("Santiago Centro")
                .medicamentos(List.of(new DetalleMedicamento("Ibuprofeno 400mg", 2)))
                .estado(estado)
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    @Test
    void crearRecetaQuedaPendienteDeReservaYPublicaElMensaje() {
        autenticarComoMedico("medico1");
        when(recetaRepository.save(any(Receta.class))).thenAnswer(inv -> {
            Receta r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        RecetaResponse response = recetaService.crearReceta(recetaRequest());

        assertThat(response.getEstado()).isEqualTo(EstadoReceta.ACEPTADA_PENDIENTE_RESERVA);
        // El username del médico viene del JWT, no del body.
        assertThat(response.getMedicoUsername()).isEqualTo("medico1");

        ArgumentCaptor<ReservaStockMessage> captor = ArgumentCaptor.forClass(ReservaStockMessage.class);
        verify(reservaStockPublisher).publicar(captor.capture());
        assertThat(captor.getValue().getRecetaId()).isEqualTo(1L);
        assertThat(captor.getValue().getSucursal()).isEqualTo("Santiago Centro");
        assertThat(captor.getValue().getMedicamentos()).hasSize(1);
    }

    @Test
    void listarSinFiltroDevuelveTodasLasRecetas() {
        when(recetaRepository.findAll()).thenReturn(List.of(
                recetaGuardada(EstadoReceta.RESERVADA),
                recetaGuardada(EstadoReceta.SIN_STOCK)));

        List<RecetaResponse> recetas = recetaService.listar(null);

        assertThat(recetas).hasSize(2);
        verify(recetaRepository).findAll();
        verify(recetaRepository, never()).findByEstado(any());
    }

    @Test
    void listarConFiltroUsaElRepositorioPorEstado() {
        when(recetaRepository.findByEstado(EstadoReceta.RESERVADA))
                .thenReturn(List.of(recetaGuardada(EstadoReceta.RESERVADA)));

        List<RecetaResponse> recetas = recetaService.listar(EstadoReceta.RESERVADA);

        assertThat(recetas).hasSize(1);
        assertThat(recetas.get(0).getEstado()).isEqualTo(EstadoReceta.RESERVADA);
    }

    @Test
    void actualizarEstadoCambiaElEstadoDeLaReceta() {
        when(recetaRepository.findById(1L))
                .thenReturn(Optional.of(recetaGuardada(EstadoReceta.ACEPTADA_PENDIENTE_RESERVA)));
        when(recetaRepository.save(any(Receta.class))).thenAnswer(inv -> inv.getArgument(0));

        RecetaResponse response = recetaService.actualizarEstado(1L, EstadoReceta.RESERVADA);

        assertThat(response.getEstado()).isEqualTo(EstadoReceta.RESERVADA);
    }

    @Test
    void actualizarRecetaReservadaNoPermiteModificarla() {
        when(recetaRepository.findById(1L))
                .thenReturn(Optional.of(recetaGuardada(EstadoReceta.RESERVADA)));

        RecetaUpdateRequest request = new RecetaUpdateRequest();
        request.setPacienteNombre("Otro Paciente");
        request.setSucursal("Providencia");
        request.setMedicamentos(List.of(new DetalleMedicamentoDto("Paracetamol 500mg", 1)));

        assertThatThrownBy(() -> recetaService.actualizarReceta(1L, request))
                .isInstanceOf(RecetaNoModificableException.class);

        verify(recetaRepository, never()).save(any());
    }

    @Test
    void eliminarRecetaDispensadaNoPermiteEliminarla() {
        when(recetaRepository.findById(1L))
                .thenReturn(Optional.of(recetaGuardada(EstadoReceta.DISPENSADA)));

        assertThatThrownBy(() -> recetaService.eliminarReceta(1L))
                .isInstanceOf(RecetaNoModificableException.class);

        verify(recetaRepository, never()).delete(any());
    }

    @Test
    void obtenerRecetaInexistenteLanzaError() {
        when(recetaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recetaService.obtenerPorId(99L))
                .isInstanceOf(RecetaNotFoundException.class);
    }
}
