package platform.common.dto;

import java.util.Map;

/**
 * Corps de {@code POST /ressource/search} : <strong>les filtres seuls</strong>. La pagination
 * ({@code page}, {@code limit}) et le tri ({@code sort}) passent en query params — ce sont des
 * scalaires simples, sortis du corps pour s'aligner sur les conventions REST usuelles. Les filtres
 * typés QueryDSL, eux, restent dans le corps (opérateurs, champs imbriqués, options).
 *
 * <p>Exemple : {@code POST /api/users/search?page=1&limit=20&sort=createdAt:desc,nom:asc}
 * (tri descendant sur {@code createdAt} puis ascendant sur {@code nom}) avec le corps
 * <pre>{"filters": {"nom": {"operator": "LIKE", "value": "dupont"}}}</pre>
 *
 * @param filters conditions de filtrage par champ (facultatif — corps absent ou vide ⇒ aucun filtre)
 */
public record SearchBody(Map<String, FilterCondition> filters) {

    public SearchBody {
        if (filters == null) {
            filters = Map.of();
        }
    }
}
