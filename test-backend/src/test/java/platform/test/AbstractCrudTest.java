package platform.test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Batterie CRUD <strong>nominale</strong> d'un {@code AbstractCrudController} exposant toutes les
 * opérations : 201 (create) → 200 (findOne) → 200 (search) → 200 (update) → 204 (remove). Les codes
 * d'erreur (401/403/400/404/422) sont couverts à part par {@link SecurityTest}, pour garder chaque
 * batterie focalisée et réutilisable.
 *
 * <p>Un projet l'étend en fournissant chemin de base et payloads :
 * <pre>{@code
 * class WidgetCrudTest extends AbstractCrudTest {
 *     protected String basePath()        { return "/api/widgets"; }
 *     protected String validCreateJson() { return "{\"name\":\"x\",\"quantity\":1}"; }
 * }
 * }</pre>
 *
 * <p>Authentification via le {@link IAuthMock} de la base : {@link #writeAuth()} (ADMIN) pour les
 * écritures, {@link #readAuth()} (USER) pour les lectures — surchargeables.
 */
public abstract class AbstractCrudTest extends PlatformIntegrationTest {

    /** Chemin de base de la ressource (ex : {@code /api/widgets}). */
    protected abstract String basePath();

    /** Corps JSON valide pour la création (201). */
    protected abstract String validCreateJson();

    /** Corps JSON valide pour la mise à jour. Par défaut, identique à la création. */
    protected String validUpdateJson() {
        return validCreateJson();
    }

    /** Auth des écritures (create/update/remove). Doit avoir les droits. Défaut : ADMIN. */
    protected RequestPostProcessor writeAuth() {
        return asAdmin();
    }

    /** Auth des lectures. Défaut : USER. */
    protected RequestPostProcessor readAuth() {
        return asUser();
    }

    @Test
    void create_returns201_withId() throws Exception {
        mockMvc.perform(post(basePath()).with(writeAuth())
                .contentType(MediaType.APPLICATION_JSON).content(validCreateJson()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void findOne_returns200() throws Exception {
        String id = createAndGetId();
        mockMvc.perform(get(basePath() + "/" + id).with(readAuth()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void search_returns200_withPaginatedBody() throws Exception {
        createAndGetId();
        // Pagination + tri en query params (format platform : `champ:desc` / `champ:asc`) ; corps réservé aux filtres.
        mockMvc.perform(post(basePath() + "/search?page=1&limit=10&sort=id:desc").with(readAuth()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.pagination.total").isNumber());
    }

    @Test
    void update_returns200() throws Exception {
        String id = createAndGetId();
        mockMvc.perform(patch(basePath() + "/" + id).with(writeAuth())
                .contentType(MediaType.APPLICATION_JSON).content(validUpdateJson()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void remove_returns204_thenFindOne404() throws Exception {
        String id = createAndGetId();
        mockMvc.perform(delete(basePath() + "/" + id).with(writeAuth()))
            .andExpect(status().isNoContent());
        mockMvc.perform(get(basePath() + "/" + id).with(readAuth()))
            .andExpect(status().isNotFound());
    }

    /** Crée une ressource via l'API et retourne son identifiant. */
    protected String createAndGetId() throws Exception {
        String body = mockMvc.perform(post(basePath()).with(writeAuth())
                .contentType(MediaType.APPLICATION_JSON).content(validCreateJson()))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("La réponse de création ne contient pas d'id : " + body);
        }
        return id;
    }
}
