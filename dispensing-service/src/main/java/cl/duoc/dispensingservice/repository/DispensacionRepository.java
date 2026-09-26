package cl.duoc.dispensingservice.repository;

import cl.duoc.dispensingservice.model.Dispensacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DispensacionRepository extends JpaRepository<Dispensacion, Long> {
    Optional<Dispensacion> findByRecetaId(Long recetaId);
    

}
