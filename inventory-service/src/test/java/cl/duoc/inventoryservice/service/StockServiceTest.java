package cl.duoc.inventoryservice.service;

import cl.duoc.inventoryservice.dto.DetalleMedicamentoMensaje;
import cl.duoc.inventoryservice.model.StockMedicamento;
import cl.duoc.inventoryservice.repository.StockMedicamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockMedicamentoRepository stockRepository;

    @InjectMocks
    private StockService stockService;

    private StockMedicamento stock(String nombre, int cantidad) {
        return StockMedicamento.builder()
                .id(1L)
                .nombreMedicamento(nombre)
                .sucursal("Santiago Centro")
                .cantidadDisponible(cantidad)
                .build();
    }

    @Test
    void hayStockSuficienteCuandoHayCantidadParaTodosLosMedicamentos() {
        when(stockRepository.findByNombreMedicamentoAndSucursal("Ibuprofeno 400mg", "Santiago Centro"))
                .thenReturn(Optional.of(stock("Ibuprofeno 400mg", 10)));
        when(stockRepository.findByNombreMedicamentoAndSucursal("Paracetamol 500mg", "Santiago Centro"))
                .thenReturn(Optional.of(stock("Paracetamol 500mg", 5)));

        boolean resultado = stockService.hayStockSuficiente("Santiago Centro", List.of(
                new DetalleMedicamentoMensaje("Ibuprofeno 400mg", 2),
                new DetalleMedicamentoMensaje("Paracetamol 500mg", 3)));

        assertThat(resultado).isTrue();
        // Solo verifica: no descuenta nada todavía.
        verify(stockRepository, never()).save(any());
    }

    @Test
    void noHayStockCuandoFaltaCantidadDeAlgUnMedicamento() {
        when(stockRepository.findByNombreMedicamentoAndSucursal("Ibuprofeno 400mg", "Santiago Centro"))
                .thenReturn(Optional.of(stock("Ibuprofeno 400mg", 1)));

        boolean resultado = stockService.hayStockSuficiente("Santiago Centro", List.of(
                new DetalleMedicamentoMensaje("Ibuprofeno 400mg", 2),
                new DetalleMedicamentoMensaje("Paracetamol 500mg", 3)));

        assertThat(resultado).isFalse();
    }

    @Test
    void noHayStockCuandoElMedicamentoNoExisteEnLaSucursal() {
        when(stockRepository.findByNombreMedicamentoAndSucursal("Amoxicilina 500mg", "Santiago Centro"))
                .thenReturn(Optional.empty());

        boolean resultado = stockService.hayStockSuficiente("Santiago Centro",
                List.of(new DetalleMedicamentoMensaje("Amoxicilina 500mg", 1)));

        assertThat(resultado).isFalse();
    }

    @Test
    void reservarStockDescuentaLaCantidadDisponible() {
        when(stockRepository.findByNombreMedicamentoAndSucursal("Ibuprofeno 400mg", "Santiago Centro"))
                .thenReturn(Optional.of(stock("Ibuprofeno 400mg", 10)));
        when(stockRepository.save(any(StockMedicamento.class))).thenAnswer(inv -> inv.getArgument(0));

        stockService.reservarStock("Santiago Centro",
                List.of(new DetalleMedicamentoMensaje("Ibuprofeno 400mg", 3)));

        ArgumentCaptor<StockMedicamento> captor = ArgumentCaptor.forClass(StockMedicamento.class);
        verify(stockRepository).save(captor.capture());
        assertThat(captor.getValue().getCantidadDisponible()).isEqualTo(7);
    }

    @Test
    void reservarStockSinFilasLanzaErrorDeNegocio() {
        when(stockRepository.findByNombreMedicamentoAndSucursal("Ibuprofeno 400mg", "Santiago Centro"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.reservarStock("Santiago Centro",
                List.of(new DetalleMedicamentoMensaje("Ibuprofeno 400mg", 3))))
                .isInstanceOf(IllegalStateException.class);

        verify(stockRepository, never()).save(any());
    }
}
