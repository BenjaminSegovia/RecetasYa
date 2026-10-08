package cl.duoc.recetaservice.exception;

import cl.duoc.recetaservice.model.EstadoReceta;

public class RecetaNoModificableException extends RuntimeException {

    public RecetaNoModificableException(Long id, EstadoReceta estado) {
        super("La receta con id " + id + " esta en estado " + estado
                + " y no puede ser modificada ni eliminada");
    }
}
