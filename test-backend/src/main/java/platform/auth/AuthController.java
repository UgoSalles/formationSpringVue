package platform.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Contrôleur d'authentification BFF générique : orchestre le flux OAuth2 Authorization Code avec
 * l'IdP (endpoints dérivés de la configuration, aucun couplage à un fournisseur précis).
 * L'échange code → tokens (confidentiel, nécessite le client_secret) se fait ici ; les tokens
 * sont posés en cookies HttpOnly du domaine projet et ne transitent jamais par le JavaScript.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String ACCESS_COOKIE = "access_token";
    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String STATE_COOKIE = "auth_state";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PlatformAuthProperties props;
    private final AuthClient client;

    public AuthController(PlatformAuthProperties props, AuthClient client) {
        this.props = props;
        this.client = client;
    }

    /**
     * Démarre la connexion : redirige vers la page d'autorisation de l'IdP.
     */
    @GetMapping("/login")
    public void login(HttpServletRequest request, HttpServletResponse response) {
        String state = randomToken();
        response.addHeader(HttpHeaders.SET_COOKIE, stateCookie(request, state, props.getRefreshMaxAge()).toString());
        String location = UriComponentsBuilder.fromUriString(props.authorizeUri())
            .queryParam("client_id", props.getClientId())
            .queryParam("redirect_uri", callbackUri())
            .queryParam("state", state)
            .build().toUriString();
        redirect(response, location);
    }

    /**
     * Callback OAuth2 : valide l'état, échange le code contre des tokens, pose les cookies,
     * puis redirige vers le front.
     */
    @GetMapping("/callback")
    public void callback(@RequestParam(required = false) String code,
                         @RequestParam(required = false) String state,
                         @CookieValue(value = STATE_COOKIE, required = false) String expectedState,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        // Invalide le cookie d'état dans tous les cas.
        response.addHeader(HttpHeaders.SET_COOKIE, stateCookie(request, "", 0).toString());

        if (code == null || state == null || expectedState == null || !expectedState.equals(state)) {
            redirect(response, props.getFrontendUrl());
            return;
        }
        AuthTokens tokens = client.exchangeCode(code, callbackUri());
        setAuthCookies(request, response, tokens);
        redirect(response, props.getFrontendUrl());
    }

    /**
     * Rafraîchit les tokens à partir du cookie de refresh. 204 si OK, 401 sinon.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(@CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        try {
            AuthTokens tokens = client.refresh(refreshToken);
            setAuthCookies(request, response, tokens);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            clearAuthCookies(request, response);
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * Déconnecte : révoque le refresh token côté IdP et efface les cookies.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        client.logout(refreshToken);
        clearAuthCookies(request, response);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retourne l'identité de l'utilisateur courant, issue des claims du token validé.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", jwt.getSubject());
        user.put("login", jwt.getClaimAsString("login"));
        user.put("email", jwt.getClaimAsString("email"));
        user.put("role", jwt.getClaimAsString("role"));
        return ResponseEntity.ok(user);
    }

    /**
     * Relaie une modification de profil vers l'IdP en réinjectant le token d'accès (cookie).
     */
    @PatchMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateMe(@RequestBody(required = false) String body,
                                           @CookieValue(value = ACCESS_COOKIE, required = false) String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        return client.patchProfile(accessToken, body);
    }

    /**
     * Relaie les préférences utilisateur (langue/thème) depuis l'IdP en réinjectant le token d'accès
     * (cookie). Sert de source à l'étape « back » de la résolution de thème côté front ; tout le
     * bloc auth (donc cet endpoint) désactivé par défaut en contexte pro (l'IdP y est à brancher par le projet).
     */
    @GetMapping("/preferences")
    public ResponseEntity<String> preferences(
            @CookieValue(value = ACCESS_COOKIE, required = false) String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        return client.getPreferences(accessToken);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String callbackUri() {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/auth/callback")
            .build().toUriString();
    }

    private void setAuthCookies(HttpServletRequest request, HttpServletResponse response, AuthTokens tokens) {
        long accessMaxAge = props.getAccessMaxAge() > 0 ? props.getAccessMaxAge() : tokens.expiresIn();
        response.addHeader(HttpHeaders.SET_COOKIE,
            cookie(ACCESS_COOKIE, tokens.accessToken(), "/", accessMaxAge).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
            cookie(REFRESH_COOKIE, tokens.refreshToken(), refreshPath(request), props.getRefreshMaxAge()).toString());
    }

    private void clearAuthCookies(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(ACCESS_COOKIE, "", "/", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, "", refreshPath(request), 0).toString());
    }

    private ResponseCookie stateCookie(HttpServletRequest request, String value, long maxAge) {
        return cookie(STATE_COOKIE, value, refreshPath(request), maxAge);
    }

    private String refreshPath(HttpServletRequest request) {
        String ctx = request.getContextPath();
        return (ctx == null ? "" : ctx) + "/auth";
    }

    private ResponseCookie cookie(String name, String value, String path, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(props.isCookieSecure())
            .path(path)
            .maxAge(Duration.ofSeconds(maxAgeSeconds))
            .sameSite(props.getCookieSameSite())
            .build();
    }

    private static String randomToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static void redirect(HttpServletResponse response, String location) {
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader(HttpHeaders.LOCATION, location);
    }
}
