package cl.duoc.inventoryservice.service;

import cl.duoc.inventoryservice.dto.DetalleMedicamentoMensaje;
import cl.duoc.inventoryservice.dto.StockRequest;
import cl.duoc.inventoryservice.dto.StockResponse;
import cl.duoc.inventoryservice.model.StockMedicamento;
import cl.duoc.inventoryservice.repository.StockMedicamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final StockMedicamentoRepository stockRepository;

    /**
     * Crea el stock de un medicamento en una sucursal, o actualiza la
     * cantidad si ya existe una fila para ese medicamento+sucursal.
     */
    public StockResponse crearOActualizarStock(StockRequest request) {
        StockMedicamento stock = stockRepository
                .findByNombreMedicamentoAndSucursal(request.getNombreMedicamento(), request.getSucursal())
                .orElse(StockMedicamento.builder()
                        .nombreMedicamento(request.getNombreMedicamento())
                        .sucursal(request.getSucursal())
                        .build());

        stock.setCantidadDisponible(request.getCantidadDisponible());
        stock = stockRepository.save(stock);

        return toResponse(stock);
    }

    public List<StockResponse> listarTodo() {
        return stockRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Verifica que haya stock suficiente de TODOS los medicamentos en la
     * sucursal indicada. No descuenta nada todavía — solo responde sí/no.
     */
    public boolean hayStockSuficiente(String sucursal, List<DetalleMedicamentoMensaje> medicamentos) {
        for (DetalleMedicamentoMensaje item : medicamentos) {
            StockMedicamento stock = stockRepository
                    .findByNombreMedicamentoAndSucursal(item.getNombreMedicamento(), sucursal)
                    .orElse(null);

            if (stock == null || stock.getCantidadDisponible() < item.getCantidad()) {
                log.warn("Stock insuficiente para {} en {}", item.getNombreMedicamento(), sucursal);
                return false;
            }
        }
        return true;
    }

    /**
     * Descuenta el stock de todos los medicamentos. Se asume que ya se
     * verificó con hayStockSuficiente(...) antes de llamar a este método.
     * @Transactional: si falla a mitad de camino, revierte todo (no queda
     * la mitad de los medicamentos descontados y la otra mitad no).
     */
    @Transactional
    public void reservarStock(String sucursal, List<DetalleMedicamentoMensaje> medicamentos) {
        for (DetalleMedicamentoMensaje item : medicamentos) {
            StockMedicamento stock = stockRepository
                    .findByNombreMedicamentoAndSucursal(item.getNombreMedicamento(), sucursal)
                    .orElseThrow(() -> new IllegalStateException(
                            "No existe stock para " + item.getNombreMedicamento() + " en " + sucursal));

            stock.setCantidadDisponible(stock.getCantidadDisponible() - item.getCantidad());
            stockRepository.save(stock);
        }
        log.info("Stock reservado correctamente para sucursal {}", sucursal);
    }

    private StockResponse toResponse(StockMedicamento stock) {
        return StockResponse.builder()
                .id(stock.getId())
                .nombreMedicamento(stock.getNombreMedicamento())
                .sucursal(stock.getSucursal())
                .cantidadDisponible(stock.getCantidadDisponible())
                .build();
    }
}