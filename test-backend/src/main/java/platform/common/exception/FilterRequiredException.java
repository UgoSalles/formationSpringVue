package platform.common.exception;

/**
 * Levée quand un filtre marqué {@code required = true} dans
 * {@link platform.common.annotation.Filter} est absent de la requête.
 */
public class FilterRequiredException extends RuntimeException {

    private final String field;

    public FilterRequiredException(String field) {
        super("Required filter missing: field='" + field + "'");
        this.field = field;
    }

    /** Champ obligatoire absent. */
    public String getField() { return field; }
}
