package ticketing.api.exception;

/**
 * 서비스 로직에서 정해진 오류 코드로 응답할 때 던지는 예외.
 * 전역 예외 처리기가 ErrorCode의 상태와 코드로 바꿔 응답한다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
