package ticketing.api.exception;

import org.springframework.http.HttpStatus;

/**
 * 오류 코드. 형식은 인증 서버와 같은 "도메인-HTTP상태".
 * 프론트는 code로 화면을 나누므로, 같은 HTTP 상태라도 의미가 다르면 문구로 구분한다.
 */
public enum ErrorCode {

    // 공통
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON-400", "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON-404", "요청한 주소를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON-405", "지원하지 않는 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-500", "서버 내부 오류가 발생했습니다."),

    // 인증: 인증 서버와 같은 코드. 401이면 프론트가 토큰을 재발급하고 한 번 더 요청한다
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-402", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-403", "만료된 토큰입니다."),

    // 티켓팅
    TICKET_NOT_ELIGIBLE(HttpStatus.FORBIDDEN, "TICKET-403", "신청 대상이 아니에요"),
    TICKET_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "TICKET-404", "없는 티켓팅이에요"),
    TICKET_NOT_OPEN(HttpStatus.CONFLICT, "TICKET-409", "아직 신청 시간이 아니에요"),
    TICKET_CLOSED(HttpStatus.CONFLICT, "TICKET-409", "신청이 마감되었어요"),
    TICKET_SOLD_OUT(HttpStatus.GONE, "TICKET-410", "티켓이 모두 소진되었어요"),
    TICKET_TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "TICKET-429", "너무 빨리 요청하고 있어요. 잠시 후 다시 시도해 주세요"),
    TICKET_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "TICKET-503", "잠시 후 다시 시도해 주세요");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
