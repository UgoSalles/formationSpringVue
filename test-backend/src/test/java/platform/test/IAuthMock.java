package platform.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Abstraction d'authentification de test — le <strong>seul</strong> point de couplage à l'IdP.
 * Encapsule comment forger une identité, l'attacher à une requête MockMvc, et déclarer la
 * configuration nécessaire à la validation des tokens. Les batteries ({@link AbstractCrudTest},
 * {@link SecurityTest}) et la base {@link PlatformIntegrationTest} n'en connaissent rien d'autre —
 * elles ne nomment jamais l'IdP réel.
 *
 * <p>Le platform fournit l'impl par défaut via {@link #platformDefault()} (cookie {@code access_token}
 * + JWKS d'un faux IdP local). En contexte pro, implémenter cette interface pour l'IdP de l'employeur
 * — dont le fonctionnement peut être tout autre (header, session, etc.) — et la retourner depuis
 * {@link PlatformIntegrationTest#authMock()} : aucune ligne de batterie ne change.
 */
public interface IAuthMock {

    /** Décore une requête pour qu'elle soit authentifiée avec le rôle donné (ex. {@code "USER"}, {@code "ADMIN"}). */
    RequestPostProcessor as(String role);

    /**
     * Décore une requête avec une identité présente mais invalide (token expiré/altéré) → 401 attendu.
     * Par défaut ne décore rien (équivaut à anonyme) ; surcharger pour tester explicitement le rejet.
     */
    default RequestPostProcessor invalid() {
        return request -> request;
    }

    /**
     * Contribue les propriétés de configuration nécessaires à la validation des tokens (typiquement
     * l'URL JWKS de l'IdP). No-op par défaut — une impl adossée à un faux IdP local la surcharge pour
     * y injecter l'URL de son serveur de clés.
     *
     * @param registry registre de propriétés dynamiques du contexte de test
     */
    default void contributeProperties(DynamicPropertyRegistry registry) {
        // No-op : un IdP réel (pro) n'a en général rien à injecter ici.
    }

    /**
     * Impl par défaut fournie par le platform — <strong>seul</strong> endroit du harness qui nomme
     * l'IdP. En contexte pro, cette référence est renommée (MDC → MDC) avec le reste des sources.
     *
     * @return mock d'authentification par défaut
     */
    static IAuthMock platformDefault() {
        return new MDCAuthMock();
    }
}
