package cl.duoc.recetaservice.repository;

import cl.duoc.recetaservice.model.EstadoReceta;
import cl.duoc.recetaservice.model.Receta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecetaRepository extends JpaRepository<Receta, Long> {
    List<Receta> findByEstado(EstadoReceta estado);
}
