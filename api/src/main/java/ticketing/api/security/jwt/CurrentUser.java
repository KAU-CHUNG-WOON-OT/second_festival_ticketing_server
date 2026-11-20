package ticketing.api.security.jwt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 붙이면 Authorization 헤더의 토큰을 검증해 로그인 사용자를 넣어 준다.
 * 예: {@code public ApiResponse<?> issue(@CurrentUser LoginUser user)}
 * 이 파라미터가 없는 API는 토큰 없이 호출할 수 있다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
