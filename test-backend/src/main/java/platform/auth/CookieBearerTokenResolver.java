package platform.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

import java.util.Arrays;

/**
 * Résout le token d'accès depuis le cookie {@code access_token} (modèle BFF), avec repli sur
 * l'en-tête {@code Authorization: Bearer <token>} (utile pour Swagger UI ou les clients API).
 */
public class CookieBearerTokenResolver implements BearerTokenResolver {

    /** Nom du cookie portant le token d'accès. */
    public static final String ACCESS_TOKEN_COOKIE = "access_token";

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public String resolve(HttpServletRequest request) {
        String fromCookie = fromCookie(request);
        return fromCookie != null ? fromCookie : fromHeader(request);
    }

    private String fromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
            .filter(c -> ACCESS_TOKEN_COOKIE.equals(c.getName()))
            .map(Cookie::getValue)
            .filter(v -> v != null && !v.isBlank())
            .findFirst()
            .orElse(null);
    }

    private String fromHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return (header != null && header.startsWith(BEARER_PREFIX))
            ? header.substring(BEARER_PREFIX.length())
            : null;
    }
}
