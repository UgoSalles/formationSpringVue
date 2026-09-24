package platform.common.dto;

import platform.common.annotation.FilterOperator;

/**
 * Condition de filtrage pour un champ donné dans {@code POST /search}.
 *
 * <p>Exemples JSON :
 * <pre>
 * {"operator": "LIKE",    "value": "dupont",              "options": {"anyBefore": false}}
 * {"operator": "IN",      "value": ["ACTIF", "INACTIF"]}
 * {"operator": "BETWEEN", "value": ["2024-01-01", "2024-12-31"], "options": {"includeEnd": false}}
 * {"operator": "IS_NULL"}
 * </pre>
 *
 * @param operator opérateur de comparaison (obligatoire)
 * @param value    valeur(s) — {@code List} pour IN/NOT_IN/BETWEEN, scalaire sinon,
 *                 absent pour IS_NULL/IS_NOT_NULL
 * @param options  options LIKE ou BETWEEN (facultatif, défauts appliqués si absent)
 */
public record FilterCondition(FilterOperator operator, Object value, FilterOptions options) {

    public FilterCondition {
        if (operator == null) throw new IllegalArgumentException("operator is required");
    }

    /** Options effectives : {@link FilterOptions#defaults()} si non fournies. */
    public FilterOptions effectiveOptions() {
        return options != null ? options : FilterOptions.defaults();
    }
}
