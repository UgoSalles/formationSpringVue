package platform.auth;

import org.springframework.http.HttpMethod;
import platform.auth.SecurityRule.AccessRule;
import platform.auth.SecurityRule.Authenticated;
import platform.auth.SecurityRule.PermitAll;
import platform.auth.SecurityRule.RequireRoles;

import java.util.List;

/**
 * Fabriques pour construire des {@link SecurityRule} de façon lisible, et règles par défaut
 * du platform.
 *
 * <p>Un projet peut surcharger la politique en exposant un bean
 * {@link PlatformSecurityRulesProvider}. Sans bean, {@link #defaults()} s'applique :
 * endpoints d'infrastructure publics, tout le reste authentifié.
 *
 * <pre>{@code
 * @Bean
 * PlatformSecurityRulesProvider rules() {
 *     return () -> List.of(
 *         SecurityRules.rule("/api/public/**", SecurityRules.permit()),
 *         SecurityRules.rule(HttpMethod.GET, "/api/users/**", SecurityRules.roles("ADMIN")),
 *         SecurityRules.rule("/**", SecurityRules.authenticated())
 *     );
 * }
 * }</pre>
 */
public final class SecurityRules {

    private SecurityRules() {
    }

    /** Chemins d'infrastructure ouverts par défaut (accueil, santé, doc, auth, clé publique). */
    public static final List<String> PUBLIC_PATHS = List.of(
        "/",
        "/health/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui",
        "/auth/**",
        "/.well-known/**"
    );

    /**
     * Politique par défaut : {@link #PUBLIC_PATHS} en accès libre, tout le reste authentifié.
     *
     * @return liste de règles par défaut
     */
    public static List<SecurityRule> defaults() {
        return List.of(
            rule("/",               permit()),
            rule("/health/**",      permit()),
            rule("/v3/api-docs/**", permit()),
            rule("/swagger-ui/**",  permit()),
            rule("/swagger-ui",     permit()),
            rule("/auth/**",        permit()),
            rule("/.well-known/**", permit()),
            rule("/**",             authenticated())
        );
    }

    public static SecurityRule rule(String path, AccessRule access) {
        return new SecurityRule(null, path, access);
    }

    public static SecurityRule rule(HttpMethod method, String path, AccessRule access) {
        return new SecurityRule(method, path, access);
    }

    public static AccessRule permit() {
        return new PermitAll();
    }

    public static AccessRule authenticated() {
        return new Authenticated();
    }

    public static AccessRule roles(String... roles) {
        return new RequireRoles(roles);
    }
}
