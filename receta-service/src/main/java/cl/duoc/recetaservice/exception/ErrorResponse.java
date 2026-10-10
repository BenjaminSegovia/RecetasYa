package cl.duoc.recetaservice.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<FieldErrorDetail> fieldErrors;

    @Getter
    @Builder
    public static class FieldErrorDetail {
        private String field;
        private String message;
    }
}
