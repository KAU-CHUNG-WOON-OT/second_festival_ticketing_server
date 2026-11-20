package ticketing.api.security.jwt;

import ticketing.api.domain.user.StudentType;

/**
 * 검증된 Access Token에서 꺼낸 로그인 사용자.
 * studentType은 카카오 가입 후 추가 정보를 등록하지 않았으면 null일 수 있다.
 */
public record LoginUser(
        long userId,
        StudentType studentType
) {
}
