package cl.duoc.dispensingservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DispensacionYaExisteException extends RuntimeException {
    public DispensacionYaExisteException(Long recetaId) {
        super("La receta " + recetaId + " ya fue dispensada anteriormente");
    }
}
