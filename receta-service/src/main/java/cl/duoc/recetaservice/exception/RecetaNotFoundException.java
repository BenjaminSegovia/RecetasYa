package cl.duoc.recetaservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RecetaNotFoundException extends RuntimeException{
    
      public RecetaNotFoundException(Long id) {
        super("No se encontró la receta con id " + id);
    }
}
