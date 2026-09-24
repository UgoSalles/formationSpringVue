package platform.common.dto;

/**
 * Options de filtrage pour les opérateurs {@code LIKE} et {@code BETWEEN}.
 *
 * <p>Valeurs par défaut : case-insensitive, accent-insensitive,
 * wildcard des deux côtés, bornes incluses.
 *
 * @param caseSensitive      LIKE sensible à la casse (défaut : false)
 * @param accentSensitive    LIKE sensible aux accents (défaut : false — TODO: unaccent PostgreSQL)
 * @param anyBefore          préfixe {@code %} avant la valeur — {@code %value} (défaut : true)
 * @param anyAfter           suffixe {@code %} après la valeur — {@code value%} (défaut : true)
 * @param anyBeforeAndAfter  raccourci {@code %value%} — écrase {@code anyBefore} et {@code anyAfter}
 *                           si {@code true} (défaut : null = non utilisé)
 * @param includeStart       borne inférieure incluse pour BETWEEN (défaut : true → {@code >=})
 * @param includeEnd         borne supérieure incluse pour BETWEEN (défaut : true → {@code <=})
 */
public record FilterOptions(
        Boolean caseSensitive,
        Boolean accentSensitive,
        Boolean anyBefore,
        Boolean anyAfter,
        Boolean anyBeforeAndAfter,
        Boolean includeStart,
        Boolean includeEnd
) {
    /** Options par défaut. */
    public static FilterOptions defaults() {
        return new FilterOptions(false, false, true, true, null, true, true);
    }

    public boolean effectiveCaseSensitive() { return caseSensitive != null && caseSensitive; }

    public boolean effectiveAnyBefore() {
        if (anyBeforeAndAfter != null && anyBeforeAndAfter) return true;
        return anyBefore == null || anyBefore;
    }

    public boolean effectiveAnyAfter() {
        if (anyBeforeAndAfter != null && anyBeforeAndAfter) return true;
        return anyAfter == null || anyAfter;
    }

    public boolean effectiveIncludeStart() { return includeStart == null || includeStart; }
    public boolean effectiveIncludeEnd()   { return includeEnd   == null || includeEnd; }
}
