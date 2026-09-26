package cl.duoc.dispensingservice.service;

import cl.duoc.dispensingservice.dto.DispensacionResponse;
import cl.duoc.dispensingservice.dto.RecetaInfo;
import cl.duoc.dispensingservice.exception.DispensacionYaExisteException;
import cl.duoc.dispensingservice.exception.RecetaNoReservadaException;
import cl.duoc.dispensingservice.model.Dispensacion;
import cl.duoc.dispensingservice.repository.DispensacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DispensacionService {
    
    private final DispensacionRepository dispensacionRepository;
    private final RecetaClient recetaClient;

    public DispensacionResponse dispensar(Long recetaId) {
        if (dispensacionRepository.findByRecetaId(recetaId).isPresent()) {
            throw new DispensacionYaExisteException(recetaId);
        }

        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String farmaceuticoUsername = jwt.getSubject();
        String token = jwt.getTokenValue();

        RecetaInfo receta = recetaClient.obtenerReceta(recetaId, token);

        if (!"RESERVADA".equals(receta.getEstado())) {
            throw new RecetaNoReservadaException(recetaId, receta.getEstado());
        }

        Dispensacion dispensacion = Dispensacion.builder()
                .recetaId(recetaId)
                .farmaceuticoUsername(farmaceuticoUsername)
                .sucursal(receta.getSucursal())
                .fechaDispensacion(LocalDateTime.now())
                .build();

        dispensacion = dispensacionRepository.save(dispensacion);

        recetaClient.marcarComoDispensada(recetaId);

        return DispensacionResponse.builder()
                .id(dispensacion.getId())
                .recetaId(dispensacion.getRecetaId())
                .farmaceuticoUsername(dispensacion.getFarmaceuticoUsername())
                .sucursal(dispensacion.getSucursal())
                .fechaDispensacion(dispensacion.getFechaDispensacion())
                .build();
    }
}
