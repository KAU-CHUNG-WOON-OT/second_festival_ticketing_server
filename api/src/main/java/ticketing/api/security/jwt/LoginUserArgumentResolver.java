package ticketing.api.security.jwt;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import ticketing.api.exception.BusinessException;
import ticketing.api.exception.ErrorCode;

/**
 * {@code @CurrentUser LoginUser} 파라미터를 채운다.
 *
 * 서블릿 필터 대신 ArgumentResolver를 쓴 이유:
 * - 로그인이 필요한 API만 이 파라미터를 선언하므로, 공개 API 경로 목록을 따로 관리하지 않아도 된다.
 * - 여기서 던진 예외는 컨트롤러 안에서 난 예외처럼 GlobalExceptionHandler가 공통 형식으로 응답한다.
 *   (필터에서 난 예외는 ControllerAdvice까지 오지 않아 응답을 직접 만들어야 한다)
 */
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenVerifier verifier;

    public LoginUserArgumentResolver(JwtTokenVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && LoginUser.class.equals(parameter.getParameterType());
    }

    @Override
    public LoginUser resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        String header = webRequest.getHeader(HttpHeaders.AUTHORIZATION);
        // 인증 서버와 같이, 토큰이 없거나 Bearer 형식이 아니면 AUTH-402
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return verifier.verify(header.substring(BEARER_PREFIX.length()).trim());
    }
}
