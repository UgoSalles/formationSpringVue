package platform.auth;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;

/**
 * Auto-configuration des services « utilisateur courant » : {@link IAuthService} et l'{@link AuditorAware}
 * d'audit qui en dépend.
 *
 * <p>Indépendante de {@code platform.auth.enabled} (contrairement à {@link AuthAutoConfiguration}) :
 * l'audit doit fonctionner même sans flux BFF actif — la requête est alors simplement anonyme. Active
 * dès que le resource-server JWT est sur le classpath ; en son absence (back sans security), aucun bean
 * n'est fourni et les colonnes d'audit restent nulles.
 *
 * <p>Un projet (mode pro avec un autre IdP, par ex.) qui expose son propre {@link IAuthService} le voit
 * pris en compte automatiquement par l'audit, sans surcharger l'{@link AuditorAware}.
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.security.oauth2.jwt.Jwt")
public class AuthSupportAutoConfiguration {

    /**
     * Service d'accès à l'utilisateur courant, adossé au JWT par défaut.
     *
     * @return implémentation par défaut basée sur le {@code SecurityContext}
     */
    @Bean
    @ConditionalOnMissingBean(IAuthService.class)
    public IAuthService platformAuthService() {
        return new MDCAuthService();
    }

    /**
     * Fournisseur d'auteur pour l'audit JPA, délégant au {@link IAuthService} disponible.
     *
     * @param authService service d'accès à l'utilisateur courant
     * @return fournisseur d'auteur pour {@code @CreatedBy}/{@code @LastModifiedBy}
     */
    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    public AuditorAware<String> platformAuditorAware(IAuthService authService) {
        return new PlatformAuditorAware(authService);
    }
}
