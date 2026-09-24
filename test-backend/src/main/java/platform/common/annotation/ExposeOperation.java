package platform.common.annotation;

/**
 * Opérations CRUD exposables via {@link Expose} sur un contrôleur.
 * {@link #NONE} est une sentinelle utilisée quand {@link Expose} déclare
 * une sous-ressource plutôt qu'une opération standard.
 */
public enum ExposeOperation {
    NONE,
    CREATE,
    FIND_ONE,
    SEARCH,
    UPDATE,
    REMOVE
}
