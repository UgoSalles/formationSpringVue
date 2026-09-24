package platform.common.filter;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import platform.common.dto.FilterCondition;
import platform.common.dto.FilterOptions;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Construit un prédicat QueryDSL {@link BooleanExpression}
 * à partir d'une map de {@link FilterCondition}.
 *
 * <p>Résolution des champs par réflexion sur la classe entité. Les champs imbriqués
 * (ex : {@code "user.name"}) sont supportés sur un niveau. Au-delà, surcharger
 * {@code buildPredicate()} dans le service.
 *
 * <p>Coercition de type : la valeur JSON (String, Integer, Double, List, etc.)
 * est convertie vers le type Java du champ cible avant construction du prédicat.
 */
public final class FilterPredicateBuilder {

    private FilterPredicateBuilder() {}

    /**
     * Construit le prédicat pour tous les filtres fournis.
     *
     * @param filters     filtres validés (champ → condition)
     * @param entityClass classe de l'entité JPA (pour la résolution des types de champ)
     * @param entityAlias alias QueryDSL (ex : "user")
     * @return expression combinée en AND, ou {@code null} si aucun filtre
     */
    public static BooleanExpression build(
            Map<String, FilterCondition> filters,
            Class<?> entityClass,
            String entityAlias) {

        if (filters == null || filters.isEmpty()) return null;

        BooleanBuilder builder = new BooleanBuilder();

        for (Map.Entry<String, FilterCondition> entry : filters.entrySet()) {
            BooleanExpression predicate = buildField(entityClass, entityAlias, entry.getKey(), entry.getValue());
            if (predicate != null) {
                builder.and(predicate);
            }
        }

        return builder.hasValue() ? Expressions.asBoolean(builder.getValue()) : null;
    }

    private static BooleanExpression buildField(
            Class<?> entityClass,
            String entityAlias,
            String fieldPath,
            FilterCondition condition) {

        String[] parts = fieldPath.split("\\.");
        Class<?> currentClass = entityClass;
        String currentAlias = entityAlias;

        // Résolution de l'imbrication (un niveau)
        for (int i = 0; i < parts.length - 1; i++) {
            Field f = findField(currentClass, parts[i]);
            if (f == null) return null;
            currentClass = f.getType();
            currentAlias = currentAlias + "." + parts[i];
        }

        String leaf = parts[parts.length - 1];
        Field field = findField(currentClass, leaf);
        if (field == null) return null;

        PathBuilder<Object> path = new PathBuilder<>(Object.class, currentAlias);
        Class<?> fieldType = field.getType();
        FilterOptions opts = condition.effectiveOptions();
        Object value = condition.value();

        return switch (condition.operator()) {
            case EQ          -> eq(path, leaf, fieldType, value);
            case NEQ         -> neq(path, leaf, fieldType, value);
            case LIKE        -> like(path, leaf, value, opts);
            case IN          -> in(path, leaf, fieldType, value);
            case NOT_IN      -> notIn(path, leaf, fieldType, value);
            case GT          -> Expressions.predicate(Ops.GT,  path.get(leaf), Expressions.constant(coerce(value, fieldType)));
            case GTE         -> Expressions.predicate(Ops.GOE, path.get(leaf), Expressions.constant(coerce(value, fieldType)));
            case LT          -> Expressions.predicate(Ops.LT,  path.get(leaf), Expressions.constant(coerce(value, fieldType)));
            case LTE         -> Expressions.predicate(Ops.LOE, path.get(leaf), Expressions.constant(coerce(value, fieldType)));
            case BETWEEN     -> between(path, leaf, fieldType, value, opts);
            case IS_NULL     -> path.get(leaf).isNull();
            case IS_NOT_NULL -> path.get(leaf).isNotNull();
        };
    }

    // ── Opérateurs ────────────────────────────────────────────────────────────

    private static BooleanExpression eq(PathBuilder<Object> path, String field,
                                         Class<?> type, Object value) {
        return Expressions.predicate(Ops.EQ, path.get(field), Expressions.constant(coerce(value, type)));
    }

    private static BooleanExpression neq(PathBuilder<Object> path, String field,
                                          Class<?> type, Object value) {
        return Expressions.predicate(Ops.NE, path.get(field), Expressions.constant(coerce(value, type)));
    }

    private static BooleanExpression like(PathBuilder<Object> path, String field,
                                           Object value, FilterOptions opts) {
        String pattern = (opts.effectiveAnyBefore() ? "%" : "")
                + str(value)
                + (opts.effectiveAnyAfter() ? "%" : "");
        // TODO: accentSensitive → unaccent() PostgreSQL
        return opts.effectiveCaseSensitive()
                ? path.getString(field).like(pattern)
                : path.getString(field).lower().like(pattern.toLowerCase());
    }

    private static BooleanExpression in(PathBuilder<Object> path, String field,
                                         Class<?> type, Object value) {
        List<Object> coerced = asList(value).stream().map(v -> coerce(v, type)).toList();
        return path.get(field).in(coerced);
    }

    private static BooleanExpression notIn(PathBuilder<Object> path, String field,
                                            Class<?> type, Object value) {
        List<Object> coerced = asList(value).stream().map(v -> coerce(v, type)).toList();
        return path.get(field).notIn(coerced);
    }

    private static BooleanExpression between(PathBuilder<Object> path, String field,
                                              Class<?> type, Object value, FilterOptions opts) {
        List<Object> bounds = asList(value);
        if (bounds.size() < 2) return null;

        Object from = coerce(bounds.get(0), type);
        Object to   = coerce(bounds.get(1), type);

        BooleanExpression lower = Expressions.predicate(
                opts.effectiveIncludeStart() ? Ops.GOE : Ops.GT,
                path.get(field), Expressions.constant(from));
        BooleanExpression upper = Expressions.predicate(
                opts.effectiveIncludeEnd() ? Ops.LOE : Ops.LT,
                path.get(field), Expressions.constant(to));

        return lower.and(upper);
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    /** Convertit la valeur JSON vers le type Java du champ cible. */
    private static Object coerce(Object value, Class<?> target) {
        if (value == null || target.isInstance(value)) return value;
        String s = value.toString();
        if (target == Integer.class   || target == int.class)    return Integer.parseInt(s);
        if (target == Long.class      || target == long.class)   return Long.parseLong(s);
        if (target == Double.class    || target == double.class) return Double.parseDouble(s);
        if (target == Float.class     || target == float.class)  return Float.parseFloat(s);
        if (target == Boolean.class   || target == boolean.class) return Boolean.parseBoolean(s);
        if (target == Instant.class)       return Instant.parse(s);
        if (target == LocalDate.class)     return LocalDate.parse(s);
        if (target == LocalDateTime.class) return LocalDateTime.parse(s);
        if (target.isEnum())               return enumFromString(target, s);
        return s;
    }

    private static Object enumFromString(Class<?> enumClass, String name) {
        try {
            return enumClass.getMethod("valueOf", String.class).invoke(null, name);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Invalid enum value '" + name + "' for " + enumClass.getSimpleName());
        }
    }

    private static List<Object> asList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(Object.class::cast).toList();
        }
        return value != null ? List.of(value) : List.of();
    }

    private static String str(Object value) {
        return value != null ? value.toString() : "";
    }

    private static Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
