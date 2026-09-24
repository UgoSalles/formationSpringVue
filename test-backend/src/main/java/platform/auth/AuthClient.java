package platform.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * Client HTTP générique vers l'IdP (OAuth2 Authorization Code) : échange de code, rafraîchissement,
 * déconnexion et proxy des modifications de profil. Centralise les appels confidentiels
 * (client_secret) côté serveur. Aucune dépendance à un fournisseur particulier : les endpoints sont
 * dérivés de la configuration ({@link PlatformAuthProperties}).
 */
public class AuthClient {

    private final PlatformAuthProperties props;
    private final RestClient restClient;

    public AuthClient(PlatformAuthProperties props, RestClient restClient) {
        this.props = props;
        this.restClient = restClient;
    }

    /**
     * Échange un code d'autorisation contre des tokens.
     *
     * @param code        code reçu sur le callback
     * @param redirectUri redirect_uri exact utilisé lors de l'autorisation
     * @return tokens émis par l'IdP
     */
    public AuthTokens exchangeCode(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("redirect_uri", redirectUri);
        return post(props.tokenUri(), form);
    }

    /**
     * Rafraîchit les tokens à partir d'un refresh token.
     *
     * @param refreshToken refresh token courant
     * @return nouveaux tokens
     */
    public AuthTokens refresh(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        return post(props.tokenUri(), form);
    }

    /**
     * Révoque le refresh token côté IdP (déconnexion).
     *
     * @param refreshToken refresh token à révoquer (peut être {@code null})
     */
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            restClient.post()
                .uri(props.logoutUri())
                .header(HttpHeaders.COOKIE, "refresh_token=" + refreshToken)
                .retrieve()
                .toBodilessEntity();
        } catch (Exception ignored) {
            // Déconnexion best-effort : on efface les cookies même si l'IdP est injoignable.
        }
    }

    /**
     * Relaie une modification de profil vers l'IdP ({@code PATCH /users/me}), en réinjectant le
     * token d'accès de l'utilisateur (le token ne quitte jamais le serveur côté navigateur).
     *
     * @param accessToken token d'accès de l'utilisateur
     * @param body        corps JSON de la requête
     * @return réponse brute de l'IdP (statut + corps)
     */
    public ResponseEntity<String> patchProfile(String accessToken, String body) {
        return restClient.patch()
            .uri(props.userInfoUri())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body == null ? "" : body)
            .exchange((req, resp) -> ResponseEntity
                .status(resp.getStatusCode())
                .body(new String(resp.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    /**
     * Lit les préférences utilisateur (langue/thème) sur l'IdP ({@code GET /preferences}), en
     * réinjectant le token d'accès de l'utilisateur (le token ne quitte jamais le serveur côté
     * navigateur). Le corps JSON de l'IdP ({@code {langue, theme}}) est relayé tel quel.
     *
     * @param accessToken token d'accès de l'utilisateur
     * @return réponse brute de l'IdP (statut + corps JSON)
     */
    public ResponseEntity<String> getPreferences(String accessToken) {
        return restClient.get()
            .uri(props.preferencesUri())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .exchange((req, resp) -> ResponseEntity
                .status(resp.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new String(resp.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    private AuthTokens post(String uri, MultiValueMap<String, String> form) {
        TokenResponse body = restClient.post()
            .uri(uri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(TokenResponse.class);
        if (body == null || body.accessToken() == null) {
            throw new ResponseStatusException(BAD_GATEWAY, "Réponse de l'IdP invalide.");
        }
        return new AuthTokens(body.accessToken(), body.refreshToken(), body.expiresIn());
    }

    /** Réponse JSON du endpoint token de l'IdP. */
    private record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("token_type") String tokenType) {}
}
