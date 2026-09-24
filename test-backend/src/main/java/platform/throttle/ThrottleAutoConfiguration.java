package platform.throttle;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration du throttle anti-flood applicatif (cf. {@link ThrottleInterceptor}).
 *
 * <p><strong>Active par défaut</strong> ({@code platform.throttle.enabled} absent ou {@code true}) :
 * barrière de sécurité de base présente dans toute appli. Débrayable via
 * {@code platform.throttle.enabled=false}. S'applique à tous les endpoints sauf l'infrastructure
 * (santé, doc API, well-known) : le login passe par l'IdP, dont la protection brute-force est traitée
 * côté MDC.
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "platform.throttle", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PlatformThrottleProperties.class)
public class ThrottleAutoConfiguration {

    /** Chemins exemptés (infra/doc). Le reste — y compris {@code /auth/**} et l'API — est throttlé. */
    private static final String[] INFRA_PATHS = {
        "/health/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/.well-known/**",
    };

    @Bean
    public ThrottleInterceptor throttleInterceptor(PlatformThrottleProperties props) {
        return new ThrottleInterceptor(props.getMinIntervalMs());
    }

    @Bean
    public ThrottleExceptionHandler throttleExceptionHandler(MessageSource messageSource) {
        return new ThrottleExceptionHandler(messageSource);
    }

    @Bean
    public WebMvcConfigurer throttleInterceptorConfigurer(ThrottleInterceptor throttleInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(throttleInterceptor)
                    .addPathPatterns("/**").excludePathPatterns(INFRA_PATHS);
            }
        };
    }
}
