package platform.test;

import jakarta.servlet.http.Cookie;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Implémentation {@link IAuthMock} pour MDC : pose un cookie {@code access_token} portant un JWT
 * RS256 signé par {@link MDCMock} et validé via son JWKS — le vrai chemin BFF du platform. C'est l'impl
 * par défaut renvoyée par {@link IAuthMock#platformDefault()}.
 */
public final class MDCAuthMock implements IAuthMock {

    @Override
    public RequestPostProcessor as(String role) {
        return cookie(TestTokens.withRole(role));
    }

    @Override
    public RequestPostProcessor invalid() {
        return cookie(TestTokens.expired("USER"));
    }

    /** Injecte l'URL de base du faux IdP (le platform en dérive le JWKS). */
    @Override
    public void contributeProperties(DynamicPropertyRegistry registry) {
        registry.add("platform.auth.url", MDCMock::startAndGetUrl);
    }

    private static RequestPostProcessor cookie(String jwt) {
        return request -> {
            request.setCookies(new Cookie("access_token", jwt));
            return request;
        };
    }
}
