package ticketing.api.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;
import ticketing.api.domain.user.StudentType;
import ticketing.api.exception.BusinessException;
import ticketing.api.exception.ErrorCode;
import ticketing.api.support.TestTokens;

class JwtTokenVerifierTest {

    private final JwtTokenVerifier verifier =
            new JwtTokenVerifier(new JwtProperties(TestTokens.SECRET, TestTokens.ISSUER));

    @Test
    void 유효한_Access_Token에서_사용자_정보를_꺼낸다() {
        LoginUser user = verifier.verify(TestTokens.access(42L, "UNDERGRADUATE"));

        assertThat(user.userId()).isEqualTo(42L);
        assertThat(user.studentType()).isEqualTo(StudentType.UNDERGRADUATE);
    }

    @Test
    void HS256으로_서명된_토큰도_검증한다() {
        LoginUser user = verifier.verify(TestTokens.hs256Access(7L, "GRADUATE"));

        assertThat(user.studentType()).isEqualTo(StudentType.GRADUATE);
    }

    @Test
    void studentType이_없으면_null로_꺼낸다() {
        LoginUser user = verifier.verify(TestTokens.access(1L, null));

        assertThat(user.studentType()).isNull();
    }

    @Test
    void 모르는_studentType은_null로_꺼낸다() {
        LoginUser user = verifier.verify(TestTokens.access(1L, "PROFESSOR"));

        assertThat(user.studentType()).isNull();
    }

    @Test
    void Refresh_Token은_거부한다() {
        assertError(TestTokens.refresh(1L, "UNDERGRADUATE"), ErrorCode.INVALID_TOKEN);
    }

    @Test
    void 만료된_토큰은_EXPIRED_TOKEN() {
        assertError(TestTokens.expired(1L, "UNDERGRADUATE"), ErrorCode.EXPIRED_TOKEN);
    }

    @Test
    void 다른_발급자의_토큰은_거부한다() {
        assertError(TestTokens.withIssuer(1L, "UNDERGRADUATE", "someone-else"), ErrorCode.INVALID_TOKEN);
    }

    @Test
    void 다른_키로_서명된_토큰은_거부한다() {
        String other = "another-secret-that-is-also-long-enough-for-hmac-sha-256-and-512-keys!!";
        assertError(TestTokens.signedWith(other, 1L, "UNDERGRADUATE"), ErrorCode.INVALID_TOKEN);
    }

    @Test
    void 형식이_깨진_토큰은_거부한다() {
        assertError("not-a-jwt", ErrorCode.INVALID_TOKEN);
    }

    @Test
    void userId_클레임이_없으면_거부한다() {
        assertError(TestTokens.withoutUserId("UNDERGRADUATE"), ErrorCode.INVALID_TOKEN);
    }

    @Test
    void 시크릿이_비어_있으면_서버가_뜨지_않는다() {
        assertThatThrownBy(() -> new JwtTokenVerifier(new JwtProperties("", TestTokens.ISSUER)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 시크릿이_32바이트보다_짧으면_서버가_뜨지_않는다() {
        assertThatThrownBy(() -> new JwtTokenVerifier(new JwtProperties("too-short", TestTokens.ISSUER)))
                .isInstanceOf(WeakKeyException.class);
    }

    private void assertError(String token, ErrorCode expected) {
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
