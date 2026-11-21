package ticketing.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Arrays;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ticketing.api.security.jwt.CurrentUser;

/**
 * Swagger(OpenAPI) 문서 설정.
 *
 * 로그인이 필요한 API는 {@code @CurrentUser} 파라미터로 구분한다(인증 방식과 같은 기준).
 * 그래서 경로 목록을 따로 적지 않아도, 그 파라미터가 있는 API에만 자물쇠 표시가 붙는다.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    static {
        // @CurrentUser LoginUser는 토큰에서 만들어지는 값이라 요청 파라미터로 보이면 안 된다
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class);
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("축제 티켓팅 API")
                        .version("v1")
                        .description("인증 서버에서 받은 Access Token을 Authorize 버튼에 넣고 호출하세요."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }

    @Bean
    public OperationCustomizer loginRequiredOperation() {
        return (operation, handlerMethod) -> {
            boolean loginRequired = Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(parameter -> parameter.hasParameterAnnotation(CurrentUser.class));
            if (loginRequired) {
                operation.addSecurityItem(new SecurityRequirement().addList(BEARER));
            }
            return operation;
        };
    }
}
