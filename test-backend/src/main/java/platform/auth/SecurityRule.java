package platform.auth;

import org.springframework.http.HttpMethod;

/**
 * Règle de sécurité déclarative : associe un chemin (et éventuellement une méthode HTTP)
 * à une politique d'accès. Les règles sont évaluées dans l'ordre, première correspondance
 * appliquée. Construire les règles via les fabriques de {@link SecurityRules}.
 *
 * @param method méthode HTTP ciblée, ou {@code null} pour toutes les méthodes
 * @param path   pattern de chemin (style Ant, ex : {@code /api/users/**})
 * @param access politique d'accès appliquée
 */
public record SecurityRule(HttpMethod method, String path, AccessRule access) {

    /** Politique d'accès d'une règle. */
    public sealed interface AccessRule permits PermitAll, Authenticated, RequireRoles {}

    /** Accès libre (sans authentification). */
    public record PermitAll() implements AccessRule {}

    /** Accès réservé aux utilisateurs authentifiés. */
    public record Authenticated() implements AccessRule {}

    /** Accès réservé aux utilisateurs possédant l'un des rôles indiqués. */
    public record RequireRoles(String... roles) implements AccessRule {}
}
