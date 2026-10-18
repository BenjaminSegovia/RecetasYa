package cl.duoc.recetaservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcOperations;

import cl.duoc.recetaservice.model.EstadoReceta;

@ExtendWith(MockitoExtension.class)
class EstadoRecetaConstraintMigrationTest {

    @Mock
    private JdbcOperations jdbc;

    @InjectMocks
    private EstadoRecetaConstraintMigration migration;

    @Test
    void debeRecrearElConstraintConTodosLosEstadosDelEnum() {
        migration.run(null);

        // Primero se borra el constraint viejo y después se crea con los
        // estados actuales del enum (incluye SIN_STOCK).
        InOrder enOrden = inOrder(jdbc);
        enOrden.verify(jdbc).execute(
                "ALTER TABLE recetas DROP CONSTRAINT IF EXISTS recetas_estado_check");
        enOrden.verify(jdbc).execute(
                "ALTER TABLE recetas ADD CONSTRAINT recetas_estado_check "
                        + "CHECK (estado IN ('ACEPTADA_PENDIENTE_RESERVA','RESERVADA','SIN_STOCK','DISPENSADA'))");
    }

    @Test
    void elConstraintDebeCubrirTodosLosValoresDelEnum() {
        migration.run(null);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc, org.mockito.Mockito.times(2)).execute(sql.capture());

        // Si mañana se agrega un estado al enum, esta prueba obliga a que el
        // constraint lo incluya (el SQL se arma desde el enum).
        String crear = sql.getAllValues().stream()
                .filter(sentencia -> sentencia.startsWith("ALTER TABLE recetas ADD"))
                .findFirst()
                .orElseThrow();
        for (EstadoReceta estado : EstadoReceta.values()) {
            assertThat(crear).contains("'" + estado.name() + "'");
        }
        assertThat(crear).contains("SIN_STOCK");
    }
}
