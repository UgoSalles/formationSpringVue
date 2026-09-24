package platform.common.filter;

import platform.common.annotation.AllowedFilters;
import platform.common.annotation.Filter;
import platform.common.annotation.FilterOperator;
import platform.common.dto.FilterCondition;
import platform.common.exception.FilterNotAllowedException;
import platform.common.exception.FilterRequiredException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Valide les filtres d'un {@code SearchRequest} contre une whitelist de {@link Filter}.
 *
 * <p>La whitelist provient soit de {@code @Expose(SEARCH, allowedFilters = {...})} (recherche CRUD
 * standard), soit d'un {@link AllowedFilters} porté sur une méthode custom.
 *
 * <p>Deux niveaux de validation :
 * <ol>
 *   <li>Filtres obligatoires ({@code required = true}) : doivent être présents → 400 si absent</li>
 *   <li>Filtres envoyés : champ et opérateur doivent être déclarés → 400 si non autorisé</li>
 * </ol>
 *
 * <p>Politique : fermé par défaut. Sans filtre déclaré (tableau vide ou {@code null}), tout filtre envoyé
 * est refusé.
 */
public final class FilterValidator {

    private FilterValidator() {}

    /**
     * Valide les filtres contre l'annotation {@link AllowedFilters} d'une méthode custom.
     *
     * @param filters        filtres envoyés par le client (peut être vide)
     * @param allowedFilters annotation {@code @AllowedFilters}, ou {@code null} si non déclarée
     */
    public static void validate(
            Map<String, FilterCondition> filters,
            AllowedFilters allowedFilters) {
        validate(filters, allowedFilters != null ? allowedFilters.value() : null);
    }

    /**
     * Valide les filtres contre la whitelist déclarée.
     *
     * @param filters filtres envoyés par le client (peut être vide)
     * @param allowed filtres autorisés (ex. {@code @Expose#allowedFilters()}) ; {@code null}/vide = aucun
     * @throws FilterRequiredException   si un filtre obligatoire est absent
     * @throws FilterNotAllowedException si un champ ou opérateur n'est pas autorisé
     */
    public static void validate(
            Map<String, FilterCondition> filters,
            Filter[] allowed) {

        // Aucun filtre déclaré : si des filtres sont envoyés, tout est refusé
        if (allowed == null || allowed.length == 0) {
            if (filters != null && !filters.isEmpty()) {
                throw new FilterNotAllowedException(filters.keySet().iterator().next(), null);
            }
            return;
        }

        Map<String, Set<FilterOperator>> whitelist = buildWhitelist(allowed);
        Map<String, FilterCondition> effective = filters != null ? filters : Map.of();

        // 1. Vérifier les filtres obligatoires
        for (Filter filter : allowed) {
            if (filter.required() && !effective.containsKey(filter.field())) {
                throw new FilterRequiredException(filter.field());
            }
        }

        // 2. Vérifier que chaque filtre envoyé est autorisé
        for (Map.Entry<String, FilterCondition> entry : effective.entrySet()) {
            String field = entry.getKey();
            FilterOperator op = entry.getValue().operator();

            Set<FilterOperator> allowedOps = whitelist.get(field);
            if (allowedOps == null) {
                throw new FilterNotAllowedException(field, null);
            }
            if (!allowedOps.contains(op)) {
                throw new FilterNotAllowedException(field, op);
            }
        }
    }

    private static Map<String, Set<FilterOperator>> buildWhitelist(Filter[] allowed) {
        Map<String, Set<FilterOperator>> map = new HashMap<>();
        for (Filter filter : allowed) {
            map.put(filter.field(), Arrays.stream(filter.operators()).collect(Collectors.toSet()));
        }
        return map;
    }
}
