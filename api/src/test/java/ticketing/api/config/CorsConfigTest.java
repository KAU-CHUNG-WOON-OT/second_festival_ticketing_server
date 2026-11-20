package ticketing.api.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(CorsConfigTest.TestController.class)
@Import({CorsConfig.class, CorsConfigTest.TestController.class})
@TestPropertySource(properties = "cors.allowed-origins=https://www.chungwoon-festival.com")
class CorsConfigTest {

    private static final String FRONT = "https://www.chungwoon-festival.com";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 허용된_Origin의_preflight는_통과하고_credentials를_허용한다() throws Exception {
        mockMvc.perform(options("/api/test")
                        .header(HttpHeaders.ORIGIN, FRONT)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void 허용되지_않은_Origin은_거부한다() throws Exception {
        mockMvc.perform(options("/api/test")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void 실제_요청에도_CORS_헤더가_붙는다() throws Exception {
        mockMvc.perform(get("/api/test").header(HttpHeaders.ORIGIN, FRONT))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT));
    }

    @RestController
    static class TestController {

        @GetMapping("/api/test")
        String test() {
            return "ok";
        }
    }
}
