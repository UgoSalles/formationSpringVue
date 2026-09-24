package platform.test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * Base des tests d'intégration du platform. Charge le contexte Spring Boot complet (sécurité réelle) sur
 * une base <strong>H2 en mémoire</strong> (mode PostgreSQL, schéma construit par JPA — Flyway désactivé :
 * les scripts sont des migrations Postgres auto-générées, hors périmètre des tests). Aucun Docker requis.
 *
 * <p>L'authentification passe par un {@link IAuthMock} <strong>swappable</strong> ({@link
 * IAuthMock#platformDefault()} par défaut : cookie {@code access_token} + JWKS d'un faux IdP local).
 * En pro, surcharger {@link #authMock()} pour brancher l'IdP de l'employeur — les batteries ne
 * changent pas. Cette base ne nomme jamais l'IdP réel.
 *
 * <p>Sous-classes : disposent de {@link #mockMvc}, des raccourcis d'auth ({@link #asUser()}/
 * {@link #asAdmin()}…) <strong>et d'une surcouche</strong> pour écrire des tests custom sans verbosité —
 * requêtes JSON ({@link #postJson}…), construction de corps ({@link #json}), seed H2 ({@link #persist}),
 * lecture de réponse ({@link #idOf}/{@link #read}) et assertions ProblemDetail ({@link #expectProblem}/
 * {@link #expectValidationError}).
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class PlatformIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    // ── Authentification ────────────────────────────────────────────────────────

    /** Mock par défaut du platform — instance unique pour la JVM de test (stateless). */
    private static final IAuthMock AUTH_MOCK = IAuthMock.platformDefault();

    /**
     * Mock d'authentification utilisé par les raccourcis. Surcharger pour brancher un autre IdP.
     * Défaut : {@link IAuthMock#platformDefault()} (cookie {@code access_token} signé localement,
     * validé via JWKS mock).
     */
    protected IAuthMock authMock() {
        return AUTH_MOCK;
    }

    /** Authentifie la requête en tant qu'utilisateur {@code USER}. */
    protected RequestPostProcessor asUser() {
        return authMock().as("USER");
    }

    /** Authentifie la requête en tant qu'{@code ADMIN}. */
    protected RequestPostProcessor asAdmin() {
        return authMock().as("ADMIN");
    }

    /** Authentifie la requête avec le rôle indiqué. */
    protected RequestPostProcessor asRole(String role) {
        return authMock().as(role);
    }

    /** Décore la requête avec une identité invalide/expirée (→ 401). */
    protected RequestPostProcessor invalidAuth() {
        return authMock().invalid();
    }

    // ── Requêtes JSON (auth + content-type en une ligne) ─────────────────────────

    /** {@code GET path} authentifié. */
    protected ResultActions getJson(String path, RequestPostProcessor auth) throws Exception {
        return mockMvc.perform(get(path).with(auth));
    }

    /** {@code POST path} authentifié, corps JSON. */
    protected ResultActions postJson(String path, String body, RequestPostProcessor auth) throws Exception {
        return mockMvc.perform(post(path).with(auth)
            .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** {@code PATCH path} authentifié, corps JSON. */
    protected ResultActions patchJson(String path, String body, RequestPostProcessor auth) throws Exception {
        return mockMvc.perform(patch(path).with(auth)
            .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** {@code DELETE path} authentifié. */
    protected ResultActions deleteJson(String path, RequestPostProcessor auth) throws Exception {
        return mockMvc.perform(delete(path).with(auth));
    }

    // ── Construction de corps JSON ───────────────────────────────────────────────

    /**
     * Construit un corps JSON à partir de paires clé/valeur :
     * {@code json("name", "gizmo", "quantity", 3)} → {@code {"name":"gizmo","quantity":3}}.
     */
    protected String json(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("json() attend des paires clé/valeur");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return objectMapper.writeValueAsString(map);
    }

    // ── Seed H2 ──────────────────────────────────────────────────────────────────

    /**
     * Persiste une entité directement en base (hors API) pour préparer l'état d'un test. Renvoie
     * l'entité gérée (identifiant uuid_v7 renseigné).
     */
    protected <T> T persist(T entity) {
        new TransactionTemplate(transactionManager)
            .executeWithoutResult(s -> entityManager.persist(entity));
        return entity;
    }

    // ── Lecture de réponse ───────────────────────────────────────────────────────

    /** Lit une valeur par JSONPath dans le corps de la réponse. */
    protected static <T> T read(ResultActions result, String jsonPath) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), jsonPath);
    }

    /** Raccourci : identifiant ({@code $.id}) renvoyé dans le corps de la réponse. */
    protected static String idOf(ResultActions result) throws Exception {
        return read(result, "$.id");
    }

    // ── Assertions ProblemDetail (RFC 9457), en une ligne ────────────────────────

    /** Vérifie un ProblemDetail : code HTTP attendu + {@code $.status} cohérent + {@code logId} présent. */
    protected ResultActions expectProblem(ResultActions result, HttpStatus expected) throws Exception {
        return result.andExpect(status().is(expected.value()))
            .andExpect(jsonPath("$.status").value(expected.value()))
            .andExpect(jsonPath("$.logId").isNotEmpty());
    }

    /** Vérifie une erreur de validation 422 portant sur le champ indiqué (tableau {@code $.errors}). */
    protected ResultActions expectValidationError(ResultActions result, String field) throws Exception {
        return result.andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.errors[?(@.field=='" + field + "')]").exists());
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // Base H2 en mémoire, compatible PostgreSQL ; schéma régénéré par JPA à la création du contexte.
        registry.add("spring.datasource.url",
            () -> "jdbc:h2:mem:platform;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        // Throttle anti-flood désactivé par défaut en test (cf. application.properties) : les batteries
        // enchaînent des appels rapides sur la même route (create/find/search…). Le défaut vit dans
        // application.properties — pas ici — pour qu'un test ciblé puisse le réactiver via
        // @TestPropertySource (qui surcharge application.properties, mais pas @DynamicPropertySource).
        // Config d'auth (JWKS) contribuée par le mock par défaut : la base ne nomme pas l'IdP.
        AUTH_MOCK.contributeProperties(registry);
        registry.add("platform.auth.client-id", () -> "test-client");
        registry.add("platform.auth.client-secret", () -> "test-secret");
        registry.add("platform.auth.cookie-secure", () -> "false");
    }
}
