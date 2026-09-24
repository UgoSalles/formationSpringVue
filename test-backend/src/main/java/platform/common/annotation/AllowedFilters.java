package platform.common.annotation;

import java.lang.annotation.*;

/**
 * Déclare les filtres QueryDSL autorisés sur un <strong>endpoint custom</strong> (méthode) qui réalise
 * lui-même une recherche. Tout champ ou opérateur non listé entraîne un 400 Bad Request (à valider via
 * {@code FilterValidator.validate(filters, methode.getAnnotation(AllowedFilters.class))}).
 *
 * <p>Pour le {@code search} CRUD standard, ne pas utiliser cette annotation : déclarer les filtres
 * directement sur {@code @Expose(value = SEARCH, allowedFilters = {...})} (cf. {@link Expose}).
 *
 * <p>Exemple sur une méthode :
 * <pre>{@code
 * @PostMapping("/recherche-avancee")
 * @AllowedFilters({
 *     @Filter(field = "nom",       operators = {LIKE}),
 *     @Filter(field = "createdAt", operators = {GT, LT, BETWEEN})
 * })
 * public ResponseEntity<...> rechercheAvancee(@RequestBody SearchBody body) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedFilters {
    Filter[] value();
}
