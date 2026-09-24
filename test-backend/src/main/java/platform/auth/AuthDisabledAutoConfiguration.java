package platform.auth;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Auto-configuration de repli active lorsque l'authentification est explicitement désactivée
 * ({@code platform.auth.enabled=false}).
 *
 * <p>Sans elle, débrancher {@link AuthAutoConfiguration} laisserait la chaîne de sécurité <em>par
 * défaut</em> de Spring Boot reprendre la main : tout endpoint fermé derrière un login généré, soit
 * l'inverse de l'intention « le back boote non connecté ». On fournit donc une
 * {@link SecurityFilterChain} stateless, CSRF désactivé, qui ouvre tous les endpoints
 * ({@code permitAll}). Les requêtes sont anonymes (l'audit retombe sur {@code anonymous}).
 *
 * <p>Cas d'usage : projet de démarrage / mode pro tant que l'IdP n'est pas branché — les développeurs
 * exercent les CRUD sans authentification. Dès que {@code platform.auth.enabled=true} (IdP configuré),
 * cette configuration s'efface au profit d'{@link AuthAutoConfiguration}.
 */
@AutoConfiguration(before = ServletWebSecurityAutoConfiguration.class)
@ConditionalOnClass(SecurityFilterChain.class)
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "platform.auth", name = "enabled", havingValue = "false")
public class AuthDisabledAutoConfiguration {

    /**
     * Chaîne de sécurité ouverte : stateless, CSRF désactivé, tous les endpoints permis. Sa présence
     * supprime la chaîne par défaut de Spring Boot (qui exigerait une authentification).
     *
     * @param http builder de sécurité
     * @return chaîne de filtres de sécurité permissive
     * @throws Exception si la configuration échoue
     */
    @Bean
    public SecurityFilterChain platformPermitAllSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
