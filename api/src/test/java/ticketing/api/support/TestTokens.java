package ticketing.api.support;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;

/**
 * 테스트용 토큰 생성기. 인증 서버의 JwtTokenProvider와 같은 클레임 구조로 만든다.
 * secret, issuer는 src/test/resources/config/application.yml과 같은 값.
 */
public final class TestTokens {

    public static final String SECRET = "test-secret-for-ticketing-api-must-be-at-least-64-bytes-long-to-allow-hs512";
    public static final String ISSUER = "auth-backend";

    private static final SecretKey KEY = key(SECRET);

    private TestTokens() {
    }

    public static String access(long userId, String studentType) {
        return base(userId, studentType).signWith(KEY).compact();
    }

    public static String refresh(long userId, String studentType) {
        return base(userId, studentType).claim("type", "refresh").signWith(KEY).compact();
    }

    public static String hs256Access(long userId, String studentType) {
        return base(userId, studentType).signWith(KEY, Jwts.SIG.HS256).compact();
    }

    public static String expired(long userId, String studentType) {
        Instant past = Instant.now().minus(Duration.ofHours(2));
        return base(userId, studentType)
                .issuedAt(Date.from(past))
                .expiration(Date.from(past.plus(Duration.ofHours(1))))
                .signWith(KEY)
                .compact();
    }

    public static String withIssuer(long userId, String studentType, String issuer) {
        return base(userId, studentType).issuer(issuer).signWith(KEY).compact();
    }

    public static String signedWith(String otherSecret, long userId, String studentType) {
        return base(userId, studentType).signWith(key(otherSecret)).compact();
    }

    public static String withoutUserId(String studentType) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("1")
                .claim("studentType", studentType)
                .issuer(ISSUER)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofHours(1))))
                .signWith(KEY)
                .compact();
    }

    private static JwtBuilder base(long userId, String studentType) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("role", "ROLE_USER")
                .claim("studentType", studentType)
                .issuer(ISSUER)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofHours(1))));
    }

    private static SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
