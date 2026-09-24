package platform.common.annotation;

import java.lang.annotation.*;

/**
 * Déclare une opération CRUD ou une sous-ressource activée sur le contrôleur.
 *
 * <p>Opération CRUD standard :
 * <pre>{@code
 * @Expose(CREATE) @Expose(FIND_ONE) @Expose(SEARCH)
 * }</pre>
 *
 * <p>Sous-ressource (génère {@code GET /{id}/{subResource}}) :
 * <pre>{@code
 * @Expose(subResource = "documents")  // → GET /dossiers/{id}/documents
 * }</pre>
 * Le nom de la sous-ressource correspond à la relation JPA de l'entité (champ {@code @OneToMany}/
 * {@code @ManyToOne}). Le routage est résolu au runtime par réflexion (cf. {@code AbstractCrudController}),
 * sans processeur d'annotations compile-time.
 *
 * <p>Filtres autorisés sur la recherche — directement sur le {@code @Expose(SEARCH)} :
 * <pre>{@code
 * @Expose(value = SEARCH, allowedFilters = {
 *     @Filter(field = "nom",    operators = {LIKE}),
 *     @Filter(field = "statut", operators = {EQ, IN}),
 * })
 * }</pre>
 * Tout champ ou opérateur non listé entraîne un 400. Pour un endpoint custom (autre que le {@code search}
 * standard), porter {@link AllowedFilters} <strong>sur la méthode</strong> et valider via
 * {@code FilterValidator}.
 *
 * <p>Les opérations non déclarées retournent 404.
 */
@Repeatable(Exposes.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Expose {
    /** Opération CRUD. {@link ExposeOperation#NONE} si c'est une sous-ressource. */
    ExposeOperation value() default ExposeOperation.NONE;
    /** Nom de la sous-ressource (ex : {@code "documents"}). Vide si c'est une opération CRUD. */
    String subResource() default "";
    /**
     * Filtres autorisés (pertinent sur {@code @Expose(SEARCH)}). Vide = aucun filtre accepté
     * (fermé par défaut). Lu par {@code AbstractCrudController.search} au runtime.
     */
    Filter[] allowedFilters() default {};
}
