package cl.duoc.recetaservice.controller;

import cl.duoc.recetaservice.dto.ActualizarEstadoRequest;
import cl.duoc.recetaservice.dto.RecetaRequest;
import cl.duoc.recetaservice.dto.RecetaResponse;
import cl.duoc.recetaservice.dto.RecetaUpdateRequest;
import cl.duoc.recetaservice.model.EstadoReceta;
import cl.duoc.recetaservice.service.RecetaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recetas")
@RequiredArgsConstructor
public class RecetaController {

    private final RecetaService recetaService;

    /**
     * Solo MEDICO. Registra la receta de inmediato con estado
     * ACEPTADA_PENDIENTE_RESERVA y dispara la reserva de stock async.
     */
    @PostMapping
    public ResponseEntity<RecetaResponse> crear(@Valid @RequestBody RecetaRequest request) {
        RecetaResponse response = recetaService.crearReceta(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * MEDICO y FARMACEUTICO. Lista todas las recetas; opcionalmente se
     * puede filtrar por estado (?estado=RESERVADA, ?estado=SIN_STOCK, etc.).
     */
    @GetMapping
    public ResponseEntity<List<RecetaResponse>> listar(
            @RequestParam(name = "estado", required = false) EstadoReceta estado) {
        return ResponseEntity.ok(recetaService.listar(estado));
    }

    /**
     * Solo FARMACEUTICO. Lista únicamente las recetas con stock ya reservado.
     */
    @GetMapping("/reservadas")
    public ResponseEntity<List<RecetaResponse>> listarReservadas() {
        return ResponseEntity.ok(recetaService.listarReservadas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecetaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(recetaService.obtenerPorId(id));
    }

    /**
     * Solo MEDICO. Actualiza una receta que aún no esté reservada ni dispensada.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecetaResponse> actualizar(@PathVariable Long id,
                                                     @Valid @RequestBody RecetaUpdateRequest request) {
        return ResponseEntity.ok(recetaService.actualizarReceta(id, request));
    }

    /**
     * Solo MEDICO. Elimina una receta que aún no esté reservada ni dispensada.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        recetaService.eliminarReceta(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Llamado internamente por inventory-service (no por médicos ni
     * farmacéuticos) para actualizar el estado tras procesar la reserva.
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<RecetaResponse> actualizarEstado(@PathVariable Long id,
                                                            @Valid @RequestBody ActualizarEstadoRequest request) {
        RecetaResponse response = recetaService.actualizarEstado(id, request.getEstado());
        return ResponseEntity.ok(response);
    }
}