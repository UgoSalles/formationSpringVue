package platform.common.exception;

import platform.common.annotation.FilterOperator;

/**
 * Levée quand un filtre envoyé dans {@code POST /search} n'est pas déclaré
 * via {@link platform.common.annotation.AllowedFilters}.
 */
public class FilterNotAllowedException extends RuntimeException {

    private final String field;
    private final FilterOperator operator;

    public FilterNotAllowedException(String field, FilterOperator operator) {
        super(operator != null
                ? "Filter not allowed: field='" + field + "', operator=" + operator
                : "Filter not allowed: field='" + field + "'");
        this.field = field;
        this.operator = operator;
    }

    /** Champ refusé. */
    public String getField() { return field; }

    /** Opérateur refusé, ou {@code null} si le champ lui-même n'est pas autorisé. */
    public FilterOperator getOperator() { return operator; }
}
