package platform.common.annotation;

import java.lang.annotation.*;

/**
 * Déclare un champ filtrable et ses opérateurs autorisés.
 * Utilisé dans {@link AllowedFilters}.
 *
 * <p>Exemple :
 * <pre>{@code
 * @Filter(field = "nom",       operators = {LIKE})
 * @Filter(field = "statut",    operators = {EQ, IN})
 * @Filter(field = "createdAt", operators = {GT, LT, BETWEEN})
 * @Filter(field = "user.name", operators = {EQ, LIKE})  // imbrication supportée
 * }</pre>
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Filter {
    /** Nom du champ (supporte l'imbrication : {@code "user.name"}). */
    String field();
    /** Opérateurs autorisés pour ce champ. */
    FilterOperator[] operators();
    /**
     * Filtre obligatoire : le champ doit être présent dans chaque requête {@code POST /search}.
     * Absence → 400. (Défaut : false)
     */
    boolean required() default false;
    /** Référence optionnelle à une règle métier ({@code "RG-X-XXX"}). */
    String rg() default "";
}
