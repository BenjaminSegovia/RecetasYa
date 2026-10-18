package cl.duoc.dispensingservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manejador centralizado de excepciones de dispensing-service.
 * Devuelve siempre el mismo formato JSON ({@link ErrorResponse}) para que el
 * frontend no tenga que interpretar varios formatos de error distintos.
 * También traduce los errores que llegan desde receta-service (llamada REST).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DispensacionYaExisteException.class)
    public ResponseEntity<ErrorResponse> handleDispensacionExistente(DispensacionYaExisteException ex,
                                                                     HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(RecetaNoReservadaException.class)
    public ResponseEntity<ErrorResponse> handleRecetaNoReservada(RecetaNoReservadaException ex,
                                                                 HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    /**
     * receta-service respondió 404: la receta pedida no existe (en vez de
     * propagar la excepción de RestClient como un 500 genérico).
     */
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleUpstream(RestClientResponseException ex,
                                                        HttpServletRequest request) {
        if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
            return build(HttpStatus.NOT_FOUND, "No existe la receta indicada en receta-service", request, null);
        }
        return build(HttpStatus.BAD_GATEWAY,
                "receta-service respondio con un error (" + ex.getStatusCode().value() + ")", request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacion(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Error de validacion en los datos enviados", request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBodyInvalido(HttpMessageNotReadableException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion tiene un formato invalido", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoInvalido(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "El parametro '" + ex.getName() + "' tiene un valor invalido", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccesoDenegado(AccessDeniedException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operacion", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenerico(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request, null);
    }

    private ErrorResponse.FieldErrorDetail toFieldError(FieldError fieldError) {
        return ErrorResponse.FieldErrorDetail.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .build();
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                               HttpServletRequest request,
                                               List<ErrorResponse.FieldErrorDetail> fieldErrors) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
