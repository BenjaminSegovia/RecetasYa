package cl.duoc.inventoryservice.controller;

import cl.duoc.inventoryservice.dto.StockRequest;
import cl.duoc.inventoryservice.dto.StockResponse;
import cl.duoc.inventoryservice.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventario")
@RequiredArgsConstructor
public class StockController {
    
    private final StockService stockService;

    @PostMapping
    public ResponseEntity<StockResponse> crearOActualizar(@Valid @RequestBody StockRequest request) {
        return ResponseEntity.ok(stockService.crearOActualizarStock(request));
    }

    @GetMapping
    public ResponseEntity<List<StockResponse>> listarTodo() {
        return ResponseEntity.ok(stockService.listarTodo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(stockService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StockResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody StockRequest request) {
        return ResponseEntity.ok(stockService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        stockService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

}
