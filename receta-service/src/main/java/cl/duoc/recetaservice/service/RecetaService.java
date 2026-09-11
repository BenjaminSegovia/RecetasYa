package cl.duoc.recetaservice.service;

import cl.duoc.recetaservice.dto.DetalleMedicamentoDto;
import cl.duoc.recetaservice.dto.RecetaRequest;
import cl.duoc.recetaservice.dto.RecetaResponse;
import cl.duoc.recetaservice.dto.ReservaStockMessage;
import cl.duoc.recetaservice.exception.RecetaNotFoundException;
import cl.duoc.recetaservice.model.DetalleMedicamento;
import cl.duoc.recetaservice.model.EstadoReceta;
import cl.duoc.recetaservice.model.Receta;
import cl.duoc.recetaservice.repository.RecetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecetaService {
    
    private final RecetaRepository recetaRepository;
    private final ReservaStockPublisher reservaStockPublisher;

    public RecetaResponse crearReceta(RecetaRequest request) {
        String medicoUsername = obtenerUsernameActual();

        List<DetalleMedicamento> detalles = request.getMedicamentos().stream()
                .map(m -> new DetalleMedicamento(m.getNombreMedicamento(), m.getCantidad()))
                .toList();

        Receta receta = Receta.builder()
                .medicoUsername(medicoUsername)
                .pacienteNombre(request.getPacienteNombre())
                .sucursal(request.getSucursal())
                .medicamentos(detalles)
                .estado(EstadoReceta.ACEPTADA_PENDIENTE_RESERVA)
                .fechaCreacion(LocalDateTime.now())
                .build();

        receta = recetaRepository.save(receta);

        // La receta queda registrada de inmediato; la reserva de stock
        // se procesa después, de forma asíncrona, sin bloquear al médico.
        reservaStockPublisher.publicar(
                ReservaStockMessage.builder()
                        .recetaId(receta.getId())
                        .sucursal(receta.getSucursal())
                        .medicamentos(request.getMedicamentos())
                        .build()
        );

        return toResponse(receta);
    }

    public List<RecetaResponse> listarReservadas() {
        return recetaRepository.findByEstado(EstadoReceta.RESERVADA).stream()
                .map(this::toResponse)
                .toList();
    }

    public RecetaResponse obtenerPorId(Long id) {
        Receta receta = recetaRepository.findById(id)
                .orElseThrow(() -> new RecetaNotFoundException(id));
        return toResponse(receta);
    }

    private String obtenerUsernameActual() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return jwt.getSubject();
    }

    private RecetaResponse toResponse(Receta receta) {
        List<DetalleMedicamentoDto> medicamentosDto = receta.getMedicamentos().stream()
                .map(m -> new DetalleMedicamentoDto(m.getNombreMedicamento(), m.getCantidad()))
                .toList();

        return RecetaResponse.builder()
                .id(receta.getId())
                .medicoUsername(receta.getMedicoUsername())
                .pacienteNombre(receta.getPacienteNombre())
                .sucursal(receta.getSucursal())
                .medicamentos(medicamentosDto)
                .estado(receta.getEstado())
                .fechaCreacion(receta.getFechaCreacion())
                .build();
    }

}
