package ticketing.api.domain.user;

/**
 * 학생 유형. 인증 서버의 StudentType과 같은 값이어야 한다.
 * 인증 서버에 값이 추가되면 여기도 함께 추가한다.
 */
public enum StudentType {
    UNDERGRADUATE,
    ON_LEAVE,
    GRADUATE;

    /**
     * 토큰의 문자열 값을 enum으로 바꾼다.
     * 값이 없거나 모르는 값이면 null (추가 정보를 등록하지 않은 사용자 등). 신청 단계에서 403으로 처리한다.
     */
    public static StudentType fromClaim(String value) {
        if (value == null) {
            return null;
        }
        for (StudentType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
