package cl.duoc.dispensingservice.controller;

import cl.duoc.dispensingservice.dto.DispensacionResponse;
import cl.duoc.dispensingservice.dto.DispensarRequest;
import cl.duoc.dispensingservice.service.DispensacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dispensaciones")
@RequiredArgsConstructor
public class DispensacionController {
    
    private final DispensacionService dispensacionService;

    /**
     * Solo FARMACEUTICO (según SecurityConfig). Verifica que la receta
     * esté RESERVADA, registra la dispensación, y avisa a receta-service
     * para que quede como DISPENSADA.
     */
    @PostMapping
    public ResponseEntity<DispensacionResponse> dispensar(@Valid @RequestBody DispensarRequest request) {
        DispensacionResponse response = dispensacionService.dispensar(request.getRecetaId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
