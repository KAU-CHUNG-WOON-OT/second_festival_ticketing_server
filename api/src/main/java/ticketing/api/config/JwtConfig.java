package ticketing.api.config;

import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ticketing.api.security.jwt.JwtProperties;
import ticketing.api.security.jwt.JwtTokenVerifier;
import ticketing.api.security.jwt.LoginUserArgumentResolver;

/**
 * JWT 설정값을 읽어 검증기를 만들고, {@code @CurrentUser} 파라미터 처리기를 등록한다.
 * 시크릿이 없거나 짧으면 서버가 뜨지 않는다.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig implements WebMvcConfigurer {

    private final JwtTokenVerifier verifier;

    public JwtConfig(JwtProperties properties) {
        this.verifier = new JwtTokenVerifier(properties);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginUserArgumentResolver(verifier));
    }
}
