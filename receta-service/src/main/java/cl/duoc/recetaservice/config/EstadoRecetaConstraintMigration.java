package cl.duoc.recetaservice.config;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Component;

import cl.duoc.recetaservice.model.EstadoReceta;

/**
 * Repara el check constraint de recetas.estado en las bases existentes.
 *
 * Al crear la tabla, Hibernate genera un CHECK con los valores que tiene el
 * enum en ese momento; con ddl-auto=update el constraint nunca se modifica.
 * Las bases creadas antes de que existiera SIN_STOCK quedaron con solo 3
 * estados, y cualquier receta sin stock explotaba con un 500 (violación de
 * constraint) en lugar de quedar marcada como SIN_STOCK.
 *
 * Este runner se ejecuta en cada arranque y recrea el constraint con los
 * estados actuales del enum: es idempotente y no toca datos.
 */
@Component
public class EstadoRecetaConstraintMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EstadoRecetaConstraintMigration.class);

    private final JdbcOperations jdbc;

    public EstadoRecetaConstraintMigration(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // La lista de estados sale del enum: si mañana se agrega otro estado,
        // el constraint lo incluye sin tocar este código.
        String estados = Arrays.stream(EstadoReceta.values())
                .map(estado -> "'" + estado.name() + "'")
                .collect(Collectors.joining(","));

        jdbc.execute("ALTER TABLE recetas DROP CONSTRAINT IF EXISTS recetas_estado_check");
        jdbc.execute("ALTER TABLE recetas ADD CONSTRAINT recetas_estado_check "
                + "CHECK (estado IN (" + estados + "))");

        log.info("Check constraint de recetas.estado actualizado: {}", estados);
    }
}
