package platform.common.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import platform.common.businessrule.BusinessRulesExporter;
import platform.common.cache.PlatformCacheResolver;
import platform.common.handler.GlobalExceptionHandler;
import platform.common.logging.ClientLogController;
import platform.common.logging.RequestLoggingFilter;

/**
 * Auto-configuration platform-back.
 *
 * <p>Fournit les beans d'infrastructure communs à toutes les applications du platform :
 * factory QueryDSL, resolver de cache, handler d'exceptions global et filtre de logging.
 */
@AutoConfiguration
@EnableJpaAuditing
public class PlatformAutoConfiguration {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Factory QueryDSL partagée, injectée dans les {@code BaseCrudService}.
     *
     * @return instance de {@code JPAQueryFactory}
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }

    /**
     * Resolver de cache platform — actif uniquement si un {@code CacheManager} est présent.
     *
     * @param cacheManager le gestionnaire de cache de l'application
     * @return instance de {@code PlatformCacheResolver}
     */
    @Bean
    @ConditionalOnBean(CacheManager.class)
    public PlatformCacheResolver platformCacheResolver(CacheManager cacheManager) {
        return new PlatformCacheResolver(cacheManager);
    }

    /**
     * Handler d'exceptions global — actif uniquement dans un contexte web.
     *
     * @param messageSource source des messages i18n
     * @return instance de {@code GlobalExceptionHandler}
     */
    @Bean
    @ConditionalOnWebApplication
    public GlobalExceptionHandler globalExceptionHandler(MessageSource messageSource) {
        return new GlobalExceptionHandler(messageSource);
    }

    /**
     * Filtre de logging des requêtes HTTP — actif uniquement dans un contexte web.
     *
     * @return instance de {@code RequestLoggingFilter}
     */
    @Bean
    @ConditionalOnWebApplication
    public RequestLoggingFilter requestLoggingFilter() {
        return new RequestLoggingFilter();
    }

    /**
     * Contrôleur de log client (POST /logs/client) — actif uniquement dans un contexte web.
     *
     * @return instance de {@code ClientLogController}
     */
    @Bean
    @ConditionalOnWebApplication
    public ClientLogController clientLogController() {
        return new ClientLogController();
    }

    /**
     * Exporteur du registre {@code business-rules.json} (règles {@code @BusinessRule} groupées par
     * entité) — actif en contexte web, débrayable via {@code platform.business-rules.enabled=false}.
     *
     * @param context contexte applicatif (scan des contrôleurs)
     * @param output  chemin de sortie (défaut {@code business-rules.json})
     * @return instance de {@code BusinessRulesExporter}
     */
    @Bean
    @ConditionalOnWebApplication
    @ConditionalOnProperty(prefix = "platform.business-rules", name = "enabled", matchIfMissing = true)
    public BusinessRulesExporter businessRulesExporter(
            ApplicationContext context,
            @Value("${platform.business-rules.output:business-rules.json}") String output) {
        return new BusinessRulesExporter(context, output);
    }
}
