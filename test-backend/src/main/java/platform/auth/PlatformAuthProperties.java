package platform.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration de l'authentification platform (connexion à l'IdP).
 *
 * <p>Un projet n'a besoin de renseigner que trois valeurs (typiquement via {@code .env}) :
 * {@code platform.auth.url}, {@code platform.auth.client-id}, {@code platform.auth.client-secret}.
 * Tous les autres paramètres ont des valeurs par défaut, et les URLs de l'IdP (authorize, token,
 * logout, jwks) sont dérivées de {@code url}.
 */
@ConfigurationProperties(prefix = "platform.auth")
public class PlatformAuthProperties {

    /** Active l'auth platform. Mettre {@code false} pour la débrayer entièrement. */
    private boolean enabled = true;

    /** URL de base de l'IdP (ex : {@code https://idp.example.com}). */
    private String url;

    /** Identifiant client de l'application enrôlée dans l'IdP. */
    private String clientId;

    /** Secret client de l'application enrôlée dans l'IdP. */
    private String clientSecret;

    /** URL vers laquelle rediriger le navigateur après connexion réussie. */
    private String frontendUrl = "/";

    /** Cookies marqués {@code Secure} (désactiver uniquement en dev HTTP local). */
    private boolean cookieSecure = true;

    /** Attribut {@code SameSite} des cookies d'authentification. */
    private String cookieSameSite = "Lax";

    /**
     * Durée de vie du cookie d'access token, en secondes (défaut : 8 heures). Pilote uniquement la
     * présence du cookie côté navigateur ; la validité réelle du token (claim {@code exp}) reste fixée
     * par l'IdP. Une valeur {@code <= 0} fait retomber sur la durée renvoyée par l'IdP ({@code expires_in}).
     */
    private long accessMaxAge = 28800;

    /** Durée de vie du cookie de refresh token, en secondes (défaut : 7 jours). */
    private long refreshMaxAge = 604800;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = stripTrailingSlash(url);
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public void setFrontendUrl(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public String getCookieSameSite() {
        return cookieSameSite;
    }

    public void setCookieSameSite(String cookieSameSite) {
        this.cookieSameSite = cookieSameSite;
    }

    public long getAccessMaxAge() {
        return accessMaxAge;
    }

    public void setAccessMaxAge(long accessMaxAge) {
        this.accessMaxAge = accessMaxAge;
    }

    public long getRefreshMaxAge() {
        return refreshMaxAge;
    }

    public void setRefreshMaxAge(long refreshMaxAge) {
        this.refreshMaxAge = refreshMaxAge;
    }

    // ── URLs de l'IdP dérivées ──────────────────────────────────────────────────

    /** @return endpoint JWKS de l'IdP pour valider les signatures de tokens */
    public String jwksUri() {
        return url + "/.well-known/jwks.json";
    }

    /** @return endpoint d'autorisation OAuth2 de l'IdP */
    public String authorizeUri() {
        return url + "/authorize";
    }

    /** @return endpoint d'échange/rafraîchissement de tokens de l'IdP */
    public String tokenUri() {
        return url + "/auth/token";
    }

    /** @return endpoint de déconnexion (révocation refresh token) de l'IdP */
    public String logoutUri() {
        return url + "/auth/logout";
    }

    /** @return endpoint du compte utilisateur sur l'IdP (proxy des modifications de profil) */
    public String userInfoUri() {
        return url + "/users/me";
    }

    /** @return endpoint des préférences utilisateur sur l'IdP (proxy lecture langue/thème) */
    public String preferencesUri() {
        return url + "/preferences";
    }

    private static String stripTrailingSlash(String url) {
        if (url == null) {
            return null;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
