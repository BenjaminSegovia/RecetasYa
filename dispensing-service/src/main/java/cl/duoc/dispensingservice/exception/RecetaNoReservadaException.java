package cl.duoc.dispensingservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class RecetaNoReservadaException extends RuntimeException {
    public RecetaNoReservadaException(Long recetaId, String estadoActual) {
        super("La receta " + recetaId + " no está reservada (estado actual: " + estadoActual + ")");
    }
    

}
