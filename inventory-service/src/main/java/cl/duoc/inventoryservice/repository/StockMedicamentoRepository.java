package cl.duoc.inventoryservice.repository;

import cl.duoc.inventoryservice.model.StockMedicamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio para la entidad StockMedicamento.
 */
public interface StockMedicamentoRepository extends JpaRepository<StockMedicamento, Long> {
    Optional<StockMedicamento> findByNombreMedicamentoAndSucursal(String nombreMedicamento, String sucursal);

}
