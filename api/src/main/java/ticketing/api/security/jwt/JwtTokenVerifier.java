package ticketing.api.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import ticketing.api.domain.user.StudentType;
import ticketing.api.exception.BusinessException;
import ticketing.api.exception.ErrorCode;

/**
 * 인증 서버가 발급한 Access Token을 검증한다.
 *
 * 인증 서버는 Access와 Refresh 토큰을 같은 키로 서명하므로,
 * 서명만 확인하면 Refresh 토큰으로도 API를 호출할 수 있게 된다. 그래서 type=refresh를 따로 거부한다.
 */
public class JwtTokenVerifier {

    private final SecretKey secretKey;
    private final String issuer;

    public JwtTokenVerifier(JwtProperties properties) {
        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException("jwt.secret(JWT_SECRET)이 설정되지 않았습니다.");
        }
        // 인증 서버와 같은 방식으로 키를 만든다. 32바이트보다 짧으면 여기서 예외가 나 서버가 뜨지 않는다
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.issuer = properties.issuer();
    }

    public LoginUser verify(String token) {
        Claims claims = parse(token);

        if ("refresh".equals(claims.get("type", String.class))) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        return new LoginUser(
                extractUserId(claims),
                StudentType.fromClaim(claims.get("studentType", String.class))
        );
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException exception) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException exception) {
            // 위조된 서명, 다른 발급자, 깨진 형식 등
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    // JSON 숫자는 크기에 따라 Integer 또는 Long으로 읽히므로 Number로 받는다
    private long extractUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        throw new BusinessException(ErrorCode.INVALID_TOKEN);
    }
}
