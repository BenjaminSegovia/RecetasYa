package cl.duoc.inventoryservice.config;

import cl.duoc.inventoryservice.model.StockMedicamento;
import cl.duoc.inventoryservice.repository.StockMedicamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Datos de prueba para desarrollo. Se activa solo con el perfil "seed"
 * (SPRING_PROFILES_ACTIVE=seed, como está en el docker-compose.yml).
 *
 * Carga stock de ejemplo en "Santiago Centro" SOLO si la tabla está vacía,
 * para no duplicar filas al reiniciar el contenedor.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SeedDataConfig {

    private static final String SUCURSAL = "Santiago Centro";

    private final StockMedicamentoRepository stockRepository;

    @Bean
    @Profile("seed")
    CommandLineRunner seedStock() {
        return args -> {
            if (stockRepository.count() > 0) {
                log.info("Ya hay stock cargado; no se agregan datos semilla");
                return;
            }

            crear("Ibuprofeno 400mg", 100);
            crear("Paracetamol 500mg", 80);
            crear("Amoxicilina 500mg", 40);
            // Este queda sin stock a propósito, para poder probar el
            // estado SIN_STOCK y la notificación al médico.
            crear("Omeprazol 20mg", 0);

            log.info("Stock semilla cargado en la sucursal '{}'", SUCURSAL);
        };
    }

    private void crear(String nombreMedicamento, int cantidad) {
        stockRepository.save(StockMedicamento.builder()
                .nombreMedicamento(nombreMedicamento)
                .sucursal(SUCURSAL)
                .cantidadDisponible(cantidad)
                .build());
    }
}
