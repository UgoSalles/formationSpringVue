package platform.common.annotation;

/** Opérateurs de filtrage autorisés dans {@link Filter}. */
public enum FilterOperator {
    LIKE,
    EQ,
    NEQ,
    IN,
    NOT_IN,
    GT,
    GTE,
    LT,
    LTE,
    BETWEEN,
    IS_NULL,
    IS_NOT_NULL
}
