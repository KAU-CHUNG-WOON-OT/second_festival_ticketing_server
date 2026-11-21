package ticketing.api.config;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ticketing.api.response.ApiResponse;
import ticketing.api.security.jwt.CurrentUser;
import ticketing.api.security.jwt.LoginUser;

/**
 * springdoc은 @WebMvcTest 범위에 들어가지 않아 @SpringBootTest로 확인한다.
 * Redis는 실제로 호출하지 않으므로 Docker 없이 돈다.
 */
class OpenApiConfigTest {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Import(DocsTestController.class)
    class 로컬_환경 {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void Bearer_인증_방식이_문서에_등록된다() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
        }

        @Test
        void 로그인이_필요한_API에만_자물쇠가_붙는다() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(jsonPath("$.paths['/api/docs-test/me'].get.security[0].bearerAuth").exists())
                    .andExpect(jsonPath("$.paths['/api/docs-test/public'].get", not(hasKey("security"))));
        }

        @Test
        void 로그인_사용자_파라미터는_요청_파라미터로_보이지_않는다() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(jsonPath("$.paths['/api/docs-test/me'].get", not(hasKey("parameters"))));
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("prod")
    class 운영_환경 {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void API_문서가_노출되지_않는다() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void Swagger_UI가_노출되지_않는다() throws Exception {
            mockMvc.perform(get("/swagger-ui.html"))
                    .andExpect(status().isNotFound());
        }
    }

    @TestConfiguration
    @RestController
    static class DocsTestController {

        @GetMapping("/api/docs-test/me")
        ApiResponse<LoginUser> me(@CurrentUser LoginUser user) {
            return ApiResponse.success("ok", user);
        }

        @GetMapping("/api/docs-test/public")
        ApiResponse<Void> publicApi() {
            return ApiResponse.success("ok");
        }
    }
}
