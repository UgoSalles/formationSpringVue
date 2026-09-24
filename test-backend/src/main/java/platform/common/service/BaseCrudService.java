package platform.common.service;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import platform.common.annotation.DefaultSort;
import platform.common.dto.*;
import platform.common.filter.FilterPredicateBuilder;
import platform.common.entity.BaseEntity;
import platform.common.exception.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service CRUD générique. Fournit search, findOne, create, update, remove
 * avec cache déclaratif via {@code platformCacheResolver} et tri par défaut
 * via {@link DefaultSort}.
 *
 * <p>Pour une méthode custom en lecture :
 * <pre>{@code
 * @Cacheable(cacheResolver = "platformCacheResolver", key = "...")
 * public MyDto findByCode(String code) { ... }
 * }</pre>
 *
 * <p>Pour une méthode custom en écriture :
 * <pre>{@code
 * @CacheEvict(cacheResolver = "platformCacheResolver", allEntries = true)
 * public void archive(UUID id) { ... }
 * }</pre>
 *
 * @param <TEntity>      entité JPA étendant {@link BaseEntity}
 * @param <TCreateDto>   DTO de création
 * @param <TUpdateDto>   DTO de mise à jour
 * @param <TResponseDto> DTO de réponse
 */
public abstract class BaseCrudService<
        TEntity extends BaseEntity,
        TCreateDto,
        TUpdateDto,
        TResponseDto> {

    // ── Abstractions ──────────────────────────────────────────────────────────

    protected abstract JpaRepository<TEntity, UUID> getRepository();
    protected abstract JPAQueryFactory getQueryFactory();
    protected abstract EntityPath<TEntity> getEntityPath();
    protected abstract Class<TEntity> getEntityClass();

    /** Nom du cache Caffeine (ex : "users"). Le cache search est "{name}:search". */
    public abstract String getCacheName();

    protected abstract TEntity toEntity(TCreateDto dto);
    protected abstract void merge(TEntity entity, TUpdateDto dto);
    protected abstract TResponseDto toDto(TEntity entity);

    /**
     * Construit le prédicat QueryDSL à partir des filtres du {@link SearchRequest}.
     * L'implémentation de base applique automatiquement les filtres validés par
     * {@code @AllowedFilters} via {@link FilterPredicateBuilder}.
     *
     * <p>Pour ajouter des contraintes supplémentaires (ex : filtre sur entité courante) :
     * <pre>{@code
     * @Override
     * protected BooleanExpression buildPredicate(SearchRequest req) {
     *     BooleanExpression base = super.buildPredicate(req);
     *     BooleanExpression extra = QUser.user.deleted.isFalse();
     *     return base != null ? base.and(extra) : extra;
     * }
     * }</pre>
     */
    protected BooleanExpression buildPredicate(SearchRequest req) {
        if (req.filters() == null || req.filters().isEmpty()) return null;
        return FilterPredicateBuilder.build(
                req.filters(),
                getEntityClass(),
                getEntityPath().getMetadata().getName());
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /**
     * Recherche paginée et triée. Pour la faible volumétrie, surcharger avec
     * {@code findAll()} sans paramètre plutôt que d'appeler cette méthode.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheResolver = "platformCacheResolver", key = "'s:' + #req.hashCode()")
    public SearchResponse<TResponseDto> search(SearchRequest req) {
        BooleanExpression predicate = buildPredicate(req);

        long total = Optional.ofNullable(
                getQueryFactory()
                        .select(Wildcard.count)
                        .from(getEntityPath())
                        .where(predicate)
                        .fetchOne()
        ).orElse(0L);

        List<TResponseDto> data = getQueryFactory()
                .selectFrom(getEntityPath())
                .where(predicate)
                .orderBy(resolveSort(req.sort()))
                .offset((long) (req.pagination().page() - 1) * req.pagination().limit())
                .limit(req.pagination().limit())
                .fetch()
                .stream().map(this::toDto).toList();

        int totalPages = (int) Math.ceil((double) total / req.pagination().limit());

        return new SearchResponse<>(data, new PaginationResponse(
                req.pagination().page(), req.pagination().limit(), total, totalPages));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheResolver = "platformCacheResolver", key = "#id")
    public TResponseDto findOne(UUID id) {
        return getRepository().findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(id.toString()));
    }

    @Transactional
    @CacheEvict(cacheResolver = "platformCacheResolver", allEntries = true)
    public TResponseDto create(TCreateDto dto) {
        return toDto(getRepository().save(toEntity(dto)));
    }

    @Transactional
    @CacheEvict(cacheResolver = "platformCacheResolver", allEntries = true)
    public TResponseDto update(UUID id, TUpdateDto dto) {
        TEntity entity = getRepository().findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id.toString()));
        merge(entity, dto);
        return toDto(getRepository().save(entity));
    }

    @Transactional
    @CacheEvict(cacheResolver = "platformCacheResolver", allEntries = true)
    public void remove(UUID id) {
        if (!getRepository().existsById(id)) {
            throw new ResourceNotFoundException(id.toString());
        }
        getRepository().deleteById(id);
    }

    // ── Sous-ressources ─────────────────────────────────────────────────────────

    /**
     * Charge une sous-ressource (relation JPA) d'une entité parente, dans une transaction de lecture
     * (les collections lazy sont donc initialisées avant la fin de transaction). Sert le routage
     * {@code @Expose(subResource = "...")} d'{@code AbstractCrudController}.
     *
     * @param id            identifiant uuid_v7 du parent
     * @param relationField nom du champ de relation (ex : {@code "documents"})
     * @return la relation : collection (copiée/détachée) ou entité liée ({@code null} possible pour un to-one)
     * @throws ResourceNotFoundException si le parent est introuvable, ou si la relation n'existe pas
     */
    @Transactional(readOnly = true)
    public Object findSubResource(UUID id, String relationField) {
        TEntity parent = getRepository().findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id.toString()));
        Object value = readRelation(parent, relationField);
        if (value instanceof Collection<?> collection) {
            return new ArrayList<>(collection); // force l'initialisation lazy avant la fin de transaction
        }
        return value;
    }

    /** Lit une relation via son getter public (compatible proxies Hibernate). */
    private static Object readRelation(Object entity, String relationField) {
        String getter = "get" + Character.toUpperCase(relationField.charAt(0)) + relationField.substring(1);
        try {
            return entity.getClass().getMethod(getter).invoke(entity);
        } catch (NoSuchMethodException e) {
            throw new ResourceNotFoundException("subResource", relationField);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Lecture de la sous-ressource '" + relationField + "' impossible", e);
        }
    }

    // ── Tri ───────────────────────────────────────────────────────────────────

    private OrderSpecifier<?>[] resolveSort(List<SortRequest> requested) {
        List<SortRequest> effective = (requested != null && !requested.isEmpty())
                ? requested
                : defaultSort();

        // Départage stable : `id` (uuid_v7, unique et monotone) est toujours ajouté en DERNIER critère.
        // Sans lui, un tri sur un champ aux valeurs dupliquées (ex. `quantity`) laisse l'ordre des
        // ex æquo indéfini → des lignes peuvent osciller entre deux pages. Le front n'a donc jamais
        // besoin de demander `id` explicitement (cf. §3.3 / §4.16).
        List<SortRequest> withTiebreaker = new ArrayList<>(effective);
        boolean alreadySortsOnId = effective.stream().anyMatch(s -> "id".equals(s.sortBy()));
        if (!alreadySortsOnId) {
            withTiebreaker.add(new SortRequest("id", SortOrder.ASC));
        }

        return withTiebreaker.stream()
                .map(this::toOrderSpecifier)
                .toArray(OrderSpecifier[]::new);
    }

    private OrderSpecifier<?> toOrderSpecifier(SortRequest s) {
        String[] parts = s.sortBy().split("\\.");
        if (parts.length > 2) {
            throw new IllegalArgumentException(
                    "Sort path '" + s.sortBy() + "' exceeds one level — override search() for deep sorting.");
        }
        String alias = getEntityPath().getMetadata().getName().toString();
        if (parts.length == 2) {
            alias = alias + "." + parts[0];
        }
        String leaf = parts[parts.length - 1];
        StringPath field = new PathBuilder<>(Object.class, alias).getString(leaf);
        return s.sortOrder() == SortOrder.DESC ? field.desc() : field.asc();
    }

    /** Lit {@link DefaultSort} sur l'entité (hérité de {@link BaseEntity} si non surchargé). */
    private List<SortRequest> defaultSort() {
        DefaultSort annotation = getEntityClass().getAnnotation(DefaultSort.class);
        return List.of(new SortRequest(annotation.field(), annotation.order()));
    }
}
