package ticketing.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 실패 응답의 error 필드.
 * retryAfterSeconds는 다시 시도할 수 있을 때까지 남은 초이며, 값이 있을 때만 응답에 포함한다.
 */
public record ErrorResponse(
        String code,
        String message,
        List<FieldErrorDetail> fieldErrors,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer retryAfterSeconds
) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of(), null);
    }

    public static ErrorResponse of(String code, String message, List<FieldErrorDetail> fieldErrors) {
        return new ErrorResponse(code, message, fieldErrors, null);
    }

    public static ErrorResponse retryAfter(String code, String message, int retryAfterSeconds) {
        return new ErrorResponse(code, message, List.of(), retryAfterSeconds);
    }

    public record FieldErrorDetail(
            String field,
            Object rejectedValue,
            String reason
    ) {
    }
}
