package platform.auth;

import java.util.List;

/**
 * Point d'extension permettant à un projet de surcharger la politique de sécurité du platform.
 * Exposer un bean de ce type pour remplacer {@link SecurityRules#defaults()}.
 */
@FunctionalInterface
public interface PlatformSecurityRulesProvider {

    /**
     * @return règles de sécurité ordonnées (première correspondance appliquée)
     */
    List<SecurityRule> rules();
}
