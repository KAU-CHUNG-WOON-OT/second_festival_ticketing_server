package ticketing.api.security.jwt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ticketing.api.config.CorsConfig;
import ticketing.api.response.ApiResponse;
import ticketing.api.support.TestTokens;

@WebMvcTest(LoginUserArgumentResolverTest.TestController.class)
@Import({LoginUserArgumentResolverTest.TestController.class, CorsConfig.class})
class LoginUserArgumentResolverTest {

    private static final String FRONT = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 유효한_토큰이면_로그인_사용자를_주입한다() throws Exception {
        mockMvc.perform(get("/api/test/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.access(42L, "ON_LEAVE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(42))
                .andExpect(jsonPath("$.data.studentType").value("ON_LEAVE"));
    }

    @Test
    void 토큰이_없으면_401_AUTH_402() throws Exception {
        mockMvc.perform(get("/api/test/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH-402"));
    }

    @Test
    void Bearer_형식이_아니면_401_AUTH_402() throws Exception {
        mockMvc.perform(get("/api/test/me").header(HttpHeaders.AUTHORIZATION, "Basic abc"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH-402"));
    }

    @Test
    void Refresh_Token이면_401_AUTH_402() throws Exception {
        mockMvc.perform(get("/api/test/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.refresh(42L, "ON_LEAVE")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH-402"));
    }

    @Test
    void 만료된_토큰이면_401_AUTH_403() throws Exception {
        mockMvc.perform(get("/api/test/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.expired(42L, "ON_LEAVE")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH-403"));
    }

    @Test
    void 로그인이_필요_없는_API는_토큰_없이_호출된다() throws Exception {
        mockMvc.perform(get("/api/test/public"))
                .andExpect(status().isOk());
    }

    @Test
    void 인증_실패_응답에도_CORS_헤더가_붙어_프론트가_읽을_수_있다() throws Exception {
        mockMvc.perform(get("/api/test/me").header(HttpHeaders.ORIGIN, FRONT))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT));
    }

    @RestController
    static class TestController {

        @GetMapping("/api/test/me")
        ApiResponse<LoginUser> me(@CurrentUser LoginUser user) {
            return ApiResponse.success("ok", user);
        }

        @GetMapping("/api/test/public")
        ApiResponse<Void> publicApi() {
            return ApiResponse.success("ok");
        }
    }
}
