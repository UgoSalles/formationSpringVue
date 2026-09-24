package platform.test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Batterie des codes d'erreur HTTP d'une ressource CRUD du platform :
 * <ul>
 *   <li><strong>401</strong> — requête anonyme et token invalide/expiré ;</li>
 *   <li><strong>400</strong> — corps JSON illisible, filtre non autorisé ;</li>
 *   <li><strong>422</strong> — corps valide mais champs invalides (Bean Validation) ;</li>
 *   <li><strong>404</strong> — ressource introuvable ;</li>
 *   <li><strong>403</strong> — rôle insuffisant (optionnel, propre à la politique du projet).</li>
 * </ul>
 *
 * <p>Authentification via le {@link IAuthMock} de la base (IdP swappable). Le projet fournit le
 * chemin et les payloads ; les tests 403 ne s'activent que si {@link #privilegedRequest()} est surchargé.
 */
public abstract class SecurityTest extends PlatformIntegrationTest {

    /** Identifiant bien formé (uuid_v7) mais inexistant. */
    protected static final String UNKNOWN_ID = "00000000-0000-7000-8000-000000000000";

    /** Chemin de base de la ressource (ex : {@code /api/widgets}). */
    protected abstract String basePath();

    /** Corps JSON syntaxiquement valide mais violant une contrainte de validation (→ 422). */
    protected abstract String invalidFieldsJson();

    /** Corps {@code POST /search} portant un filtre non déclaré dans {@code @AllowedFilters} (→ 400). */
    protected abstract String disallowedFilterJson();

    /**
     * Requête vers un endpoint à privilège, <strong>sans</strong> authentification appliquée (le test
     * ajoute le rôle). Retourner {@code null} (défaut) désactive les tests 403/rôle, la politique de
     * rôles étant propre au projet.
     */
    protected MockHttpServletRequestBuilder privilegedRequest() {
        return null;
    }

    /** Rôle disposant du privilège ci-dessus. Défaut : ADMIN. */
    protected String privilegedRole() {
        return "ADMIN";
    }

    /** Rôle n'en disposant pas. Défaut : USER. */
    protected String unprivilegedRole() {
        return "USER";
    }

    // ── 401 ─────────────────────────────────────────────────────────────────────

    @Test
    void anonymous_returns401() throws Exception {
        mockMvc.perform(get(basePath() + "/" + UNKNOWN_ID))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidToken_returns401() throws Exception {
        mockMvc.perform(get(basePath() + "/" + UNKNOWN_ID).with(invalidAuth()))
            .andExpect(status().isUnauthorized());
    }

    // ── 404 ─────────────────────────────────────────────────────────────────────

    @Test
    void unknownId_returns404() throws Exception {
        mockMvc.perform(get(basePath() + "/" + UNKNOWN_ID).with(asUser()))
            .andExpect(status().isNotFound());
    }

    // ── 400 ─────────────────────────────────────────────────────────────────────

    @Test
    void malformedJson_returns400() throws Exception {
        mockMvc.perform(post(basePath()).with(asAdmin())
                .contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void disallowedFilter_returns400() throws Exception {
        mockMvc.perform(post(basePath() + "/search").with(asUser())
                .contentType(MediaType.APPLICATION_JSON).content(disallowedFilterJson()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.field").isNotEmpty());
    }

    // ── 422 ─────────────────────────────────────────────────────────────────────

    @Test
    void invalidFields_returns422() throws Exception {
        mockMvc.perform(post(basePath()).with(asAdmin())
                .contentType(MediaType.APPLICATION_JSON).content(invalidFieldsJson()))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.errors").isArray());
    }

    // ── 403 ─────────────────────────────────────────────────────────────────────

    @Test
    void wrongRole_returns403() throws Exception {
        MockHttpServletRequestBuilder request = privilegedRequest();
        Assumptions.assumeTrue(request != null, "privilegedRequest() non surchargé — test 403 ignoré");
        mockMvc.perform(request.with(asRole(unprivilegedRole())))
            .andExpect(status().isForbidden());
    }

    @Test
    void rightRole_isNotForbidden() throws Exception {
        MockHttpServletRequestBuilder request = privilegedRequest();
        Assumptions.assumeTrue(request != null, "privilegedRequest() non surchargé — test 403 ignoré");
        mockMvc.perform(request.with(asRole(privilegedRole())))
            .andExpect(result -> {
                if (result.getResponse().getStatus() == 403) {
                    throw new AssertionError("Le rôle privilégié ne devrait pas recevoir 403");
                }
            });
    }
}
