package ticketing.api.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 jwt.* 값.
 * 인증 서버와 같은 secret, issuer여야 그쪽에서 발급한 토큰을 검증할 수 있다.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        String issuer
) {
}
