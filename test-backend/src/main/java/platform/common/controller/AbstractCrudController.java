package platform.common.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import platform.common.annotation.Expose;
import platform.common.annotation.ExposeOperation;
import platform.common.annotation.ExposeOverride;
import platform.common.annotation.Filter;
import platform.common.filter.FilterValidator;
import platform.common.dto.FilterCondition;
import platform.common.dto.PaginationRequest;
import platform.common.dto.SearchBody;
import platform.common.dto.SearchRequest;
import platform.common.dto.SearchResponse;
import platform.common.dto.SortOrder;
import platform.common.dto.SortRequest;
import platform.common.entity.BaseEntity;
import platform.common.exception.ResourceNotFoundException;
import platform.common.service.BaseCrudService;
import platform.common.validation.ValidationGroups;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contrôleur CRUD abstrait. Expose les opérations déclarées via {@link Expose} sur
 * la classe concrète. Les opérations non déclarées retournent 404.
 *
 * <p>Usage minimal :
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/users")
 * @Expose(CREATE) @Expose(FIND_ONE) @Expose(SEARCH)
 * public class UserController extends AbstractCrudController<User, UserCreateDto, UserUpdateDto, UserDetailDto> {
 *
 *     @Autowired private UserService userService;
 *
 *     @Override
 *     protected BaseCrudService<User, UserCreateDto, UserUpdateDto, UserDetailDto> getService() {
 *         return userService;
 *     }
 * }
 * }</pre>
 *
 * <p>Pour surcharger un endpoint, utiliser {@link ExposeOverride} :
 * <pre>{@code
 * @ExposeOverride(CREATE)
 * public ResponseEntity<UserDetailDto> create(@RequestBody UserCreateDto dto) {
 *     // logique avant
 *     return super.create(dto);
 * }
 * }</pre>
 *
 * <p>Les filtres search autorisés se déclarent via {@code @Expose(value = SEARCH, allowedFilters = {...})}
 * (cf. {@link Expose}). Pour un endpoint custom, porter {@code @AllowedFilters} sur la méthode.
 *
 * @param <TEntity>      entité JPA étendant {@link BaseEntity}
 * @param <TCreateDto>   DTO de création
 * @param <TUpdateDto>   DTO de mise à jour
 * @param <TResponseDto> DTO de réponse
 */
public abstract class AbstractCrudController<
        TEntity extends BaseEntity,
        TCreateDto,
        TUpdateDto,
        TResponseDto> {

    /**
     * Service délégué. Un seul service par contrôleur (contrainte APT).
     *
     * @return le service CRUD associé à cette ressource
     */
    protected abstract BaseCrudService<TEntity, TCreateDto, TUpdateDto, TResponseDto> getService();

    // ── Endpoints ─────────────────────────────────────────────────────────────

    /**
     * Crée une ressource.
     * Activé par {@code @Expose(CREATE)}.
     *
     * @param dto données de création
     * @return 201 Created avec le DTO de la ressource créée
     */
    @PostMapping
    public ResponseEntity<TResponseDto> create(
            @RequestBody @Validated(ValidationGroups.Create.class) TCreateDto dto) {
        checkExposed(ExposeOperation.CREATE);
        return ResponseEntity.status(HttpStatus.CREATED).body(getService().create(dto));
    }

    /**
     * Récupère une ressource par son identifiant uuid_v7.
     * Activé par {@code @Expose(FIND_ONE)}. Un identifiant malformé entraîne un 400.
     *
     * @param id identifiant uuid_v7
     * @return 200 OK avec le DTO, ou 404 si introuvable
     */
    @GetMapping("/{id}")
    public ResponseEntity<TResponseDto> findOne(@PathVariable UUID id) {
        checkExposed(ExposeOperation.FIND_ONE);
        return ResponseEntity.ok(getService().findOne(id));
    }

    /**
     * Recherche paginée, triée et filtrée.
     * Activé par {@code @Expose(SEARCH)}.
     *
     * <p>La pagination ({@code page}, {@code limit}) et le tri ({@code sort}) sont des query params ;
     * seuls les <strong>filtres</strong> typés vivent dans le corps ({@link SearchBody}). Format de
     * tri platform : {@code champ:direction} ({@code asc}/{@code desc}, {@code asc} par défaut), virgule
     * pour séparer les champs, param répétable ({@code ?sort=createdAt:desc,nom:asc}). Les filtres
     * autorisés se déclarent via {@code @Expose(value = SEARCH, allowedFilters = {...})} — tout champ ou
     * opérateur non déclaré entraîne un 400 (fermé par défaut : sans {@code allowedFilters}, aucun filtre).
     *
     * @param page   numéro de page (1-based, défaut 1)
     * @param limit  taille de page (défaut 10, borné par {@link PaginationRequest})
     * @param sort   critères de tri {@code champ[:asc|:desc]} répétables (facultatif → {@code @DefaultSort})
     * @param body   filtres (facultatif → aucun filtre)
     * @return 200 OK avec la page de résultats
     */
    @PostMapping("/search")
    public ResponseEntity<SearchResponse<TResponseDto>> search(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(name = "sort", required = false) List<String> sort,
            @RequestBody(required = false) SearchBody body) {
        checkExposed(ExposeOperation.SEARCH);
        Map<String, FilterCondition> filters = body != null ? body.filters() : Map.of();
        FilterValidator.validate(filters, searchAllowedFilters());
        SearchRequest req = new SearchRequest(new PaginationRequest(page, limit), parseSort(sort), filters);
        return ResponseEntity.ok(getService().search(req));
    }

    /**
     * Met à jour partiellement une ressource existante.
     * Activé par {@code @Expose(UPDATE)}.
     *
     * @param id  identifiant uuid_v7
     * @param dto données de mise à jour
     * @return 200 OK avec le DTO mis à jour, ou 404 si introuvable
     */
    @PatchMapping("/{id}")
    public ResponseEntity<TResponseDto> update(
            @PathVariable UUID id,
            @RequestBody @Validated(ValidationGroups.Update.class) TUpdateDto dto) {
        checkExposed(ExposeOperation.UPDATE);
        return ResponseEntity.ok(getService().update(id, dto));
    }

    /**
     * Supprime une ressource.
     * Activé par {@code @Expose(REMOVE)}.
     *
     * @param id identifiant uuid_v7
     * @return 204 No Content, ou 404 si introuvable
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable UUID id) {
        checkExposed(ExposeOperation.REMOVE);
        getService().remove(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Récupère une sous-ressource (relation) déclarée via {@code @Expose(subResource = "...")}.
     * Ex : {@code @Expose(subResource = "documents")} sur {@code @RequestMapping("/api/dossiers")} →
     * {@code GET /api/dossiers/{id}/documents}. Une sous-ressource non déclarée retourne 404.
     *
     * @param id          identifiant uuid_v7 du parent
     * @param subResource segment de sous-ressource (= nom de la relation sur l'entité)
     * @return 200 OK avec la ressource liée (collection ou entité)
     */
    @GetMapping("/{id}/{subResource}")
    public ResponseEntity<Object> getSubResource(
            @PathVariable UUID id, @PathVariable String subResource) {
        checkSubResourceExposed(subResource);
        return ResponseEntity.ok(getService().findSubResource(id, subResource));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Vérifie qu'une opération CRUD est déclarée via {@link Expose} sur le contrôleur concret.
     * Les déclarations de sous-ressource ({@code @Expose(subResource = ...)}) sont prises en compte
     * séparément par {@link #checkSubResourceExposed(String)}.
     *
     * @param operation opération à vérifier
     * @throws ResourceNotFoundException (404) si l'opération n'est pas exposée
     */
    protected void checkExposed(ExposeOperation operation) {
        Expose[] exposes = getClass().getAnnotationsByType(Expose.class);
        boolean exposed = Arrays.stream(exposes)
                .anyMatch(e -> e.value() == operation && e.subResource().isEmpty());
        if (!exposed) {
            throw new ResourceNotFoundException("endpoint", operation.name().toLowerCase());
        }
    }

    /**
     * Vérifie qu'une sous-ressource est déclarée via {@code @Expose(subResource = "...")} sur le
     * contrôleur concret.
     *
     * @param subResource nom de la sous-ressource (= relation de l'entité)
     * @throws ResourceNotFoundException (404) si la sous-ressource n'est pas exposée
     */
    protected void checkSubResourceExposed(String subResource) {
        boolean exposed = Arrays.stream(getClass().getAnnotationsByType(Expose.class))
                .anyMatch(e -> subResource.equals(e.subResource()));
        if (!exposed) {
            throw new ResourceNotFoundException("subResource", subResource);
        }
    }

    /**
     * Filtres autorisés sur la recherche, lus depuis {@code @Expose(SEARCH).allowedFilters()} du
     * contrôleur concret. Vide si {@code SEARCH} n'est pas exposé ou n'en déclare aucun (fermé par défaut).
     *
     * @return les filtres autorisés (jamais {@code null})
     */
    private Filter[] searchAllowedFilters() {
        return Arrays.stream(getClass().getAnnotationsByType(Expose.class))
                .filter(e -> e.value() == ExposeOperation.SEARCH && e.subResource().isEmpty())
                .map(Expose::allowedFilters)
                .findFirst()
                .orElse(new Filter[0]);
    }

    /**
     * Convertit les query params de tri en {@link SortRequest}. Format platform : {@code champ:direction},
     * où {@code direction} vaut {@code asc} (défaut si absente) ou {@code desc}, insensible à la casse
     * ({@code ?sort=createdAt:desc,nom:asc} ⇒ {@code createdAt DESC, nom ASC}). La virgule sépare les
     * champs (Spring la découpe nativement), le param est aussi répétable
     * ({@code ?sort=createdAt:desc&sort=nom}). Liste vide ⇒ tri par défaut appliqué par le service
     * ({@code @DefaultSort} sur l'entité, sinon {@code id ASC}), avec départage {@code id} systématique.
     *
     * @param sort entrées brutes {@code champ[:asc|:desc]} (peut être {@code null} ou vide)
     * @return la liste de tris, éventuellement vide
     */
    private static List<SortRequest> parseSort(List<String> sort) {
        if (sort == null || sort.isEmpty()) {
            return List.of();
        }
        return sort.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .map(AbstractCrudController::toSortRequest)
                .toList();
    }

    /**
     * Parse une entrée {@code champ[:direction]} en {@link SortRequest} : {@code :desc} ⇒ {@code DESC},
     * {@code :asc} ou direction absente ⇒ {@code ASC} (insensible à la casse).
     */
    private static SortRequest toSortRequest(String raw) {
        int sep = raw.indexOf(':');
        if (sep < 0) {
            return new SortRequest(raw, SortOrder.ASC);
        }
        String field = raw.substring(0, sep).trim();
        String direction = raw.substring(sep + 1).trim();
        SortOrder order = "desc".equalsIgnoreCase(direction) ? SortOrder.DESC : SortOrder.ASC;
        return new SortRequest(field, order);
    }
}
