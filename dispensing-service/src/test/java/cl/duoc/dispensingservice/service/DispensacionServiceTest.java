package cl.duoc.dispensingservice.service;

import cl.duoc.dispensingservice.dto.DispensacionResponse;
import cl.duoc.dispensingservice.dto.RecetaInfo;
import cl.duoc.dispensingservice.exception.DispensacionYaExisteException;
import cl.duoc.dispensingservice.exception.RecetaNoReservadaException;
import cl.duoc.dispensingservice.model.Dispensacion;
import cl.duoc.dispensingservice.repository.DispensacionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispensacionServiceTest {

    @Mock
    private DispensacionRepository dispensacionRepository;

    @Mock
    private RecetaClient recetaClient;

    @InjectMocks
    private DispensacionService dispensacionService;

    @AfterEach
    void limpiarSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /** Simula al farmacéutico autenticado. */
    private void autenticarComoFarmaceutico(String username) {
        Jwt jwt = Jwt.withTokenValue("token-farma")
                .header("alg", "HS256")
                .subject(username)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, null, List.of()));
    }

    private RecetaInfo receta(String estado) {
        RecetaInfo receta = new RecetaInfo();
        receta.setId(1L);
        receta.setSucursal("Santiago Centro");
        receta.setEstado(estado);
        return receta;
    }

    private Dispensacion dispensacionGuardada() {
        return Dispensacion.builder()
                .id(10L)
                .recetaId(1L)
                .farmaceuticoUsername("farma1")
                .sucursal("Santiago Centro")
                .fechaDispensacion(LocalDateTime.now())
                .build();
    }

    @Test
    void dispensarRecetaReservadaGuardaYMarcaComoDispensada() {
        autenticarComoFarmaceutico("farma1");
        when(dispensacionRepository.findByRecetaId(1L)).thenReturn(Optional.empty());
        when(recetaClient.obtenerReceta(1L, "token-farma")).thenReturn(receta("RESERVADA"));
        when(dispensacionRepository.save(any(Dispensacion.class))).thenReturn(dispensacionGuardada());

        DispensacionResponse response = dispensacionService.dispensar(1L);

        assertThat(response.getRecetaId()).isEqualTo(1L);
        assertThat(response.getFarmaceuticoUsername()).isEqualTo("farma1");
        assertThat(response.getSucursal()).isEqualTo("Santiago Centro");

        // Marca la receta como DISPENSADA (PATCH interno con API key).
        verify(recetaClient).marcarComoDispensada(1L);
    }

    @Test
    void dispensarRecetaYaDispensadaLanzaError() {
        autenticarComoFarmaceutico("farma1");
        when(dispensacionRepository.findByRecetaId(1L))
                .thenReturn(Optional.of(dispensacionGuardada()));

        assertThatThrownBy(() -> dispensacionService.dispensar(1L))
                .isInstanceOf(DispensacionYaExisteException.class);

        // Ni siquiera consulta ni marca la receta.
        verify(recetaClient, never()).obtenerReceta(anyLong(), anyString());
        verify(recetaClient, never()).marcarComoDispensada(anyLong());
    }

    @Test
    void dispensarRecetaNoReservadaLanzaError() {
        autenticarComoFarmaceutico("farma1");
        when(dispensacionRepository.findByRecetaId(1L)).thenReturn(Optional.empty());
        when(recetaClient.obtenerReceta(1L, "token-farma")).thenReturn(receta("SIN_STOCK"));

        assertThatThrownBy(() -> dispensacionService.dispensar(1L))
                .isInstanceOf(RecetaNoReservadaException.class);

        // No guarda ni cambia el estado si la receta no estaba reservada.
        verify(dispensacionRepository, never()).save(any());
        verify(recetaClient, never()).marcarComoDispensada(anyLong());
    }
}
