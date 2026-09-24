package platform.common.annotation;

import java.lang.annotation.*;

/**
 * Documente une règle métier portée par une méthode ou un endpoint.
 * Format du code : {@code RG-[RESSOURCE]-[NUMERO]} (ex : {@code RG-USER-001}).
 * Convention : description d'au moins 20 caractères (convention d'équipe, non vérifiée à la compilation).
 * Lue au démarrage par {@code BusinessRulesExporter} qui génère {@code business-rules.json}
 * (règles groupées par entité, versionné, exportable CSV) — réflexion runtime, pas d'APT.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessRule {
    /** Code de la règle, format RG-[RESSOURCE]-[NUMERO]. */
    String code();
    /** Description lisible (≥ 20 caractères). */
    String description();
}
