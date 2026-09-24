package platform.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Faux MDC pour les tests : il <strong>possède</strong> sa propre paire de clés RSA (la privée signe
 * les JWT de test, la publique est exposée via {@code /.well-known/jwks.json}) et démarre un serveur
 * WireMock servant ce JWKS. Le décodeur du platform y récupère la clé publique et valide les tokens RS256
 * signés ici — chemin cookie + JWKS réel de bout en bout. Détenir la clé fait partie du rôle du mock :
 * c'est « l'MDC » des tests.
 *
 * <p>Un unique serveur est démarré pour toute la JVM de test et réutilisé. Pour brancher un autre IdP
 * (ex. en pro), ne pas réutiliser cette classe : implémenter {@link IAuthMock}.
 */
public final class MDCMock {

    /** Identifiant de clé annoncé dans l'en-tête JWT et le JWKS. */
    private static final String KID = "platform-test-key";

    /** Paire RSA de test, générée une fois pour la JVM. */
    private static final RSAKey RSA_KEY = generateKey();

    private static volatile WireMockServer server;

    private MDCMock() {
    }

    /**
     * Démarre le mock (au premier appel) et retourne l'URL de base à injecter dans
     * {@code platform.auth.url}. Le platform en dérive {@code <url>/.well-known/jwks.json}.
     */
    public static String startAndGetUrl() {
        WireMockServer local = server;
        if (local == null) {
            synchronized (MDCMock.class) {
                local = server;
                if (local == null) {
                    local = new WireMockServer(options().dynamicPort());
                    local.start();
                    local.stubFor(get(urlEqualTo("/.well-known/jwks.json"))
                        .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(publicJwksJson())));
                    server = local;
                }
            }
        }
        return "http://localhost:" + local.port();
    }

    /** Signe les claims fournis en RS256 avec la clé privée de test (en-tête portant le {@code kid}). */
    public static String sign(JWTClaimsSet claims) {
        try {
            SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KID).build(), claims);
            jwt.sign(new RSASSASigner(RSA_KEY));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Échec de signature du JWT de test", e);
        }
    }

    /** JWKS (clé publique uniquement) servi par le mock. */
    private static String publicJwksJson() {
        return new JWKSet(RSA_KEY.toPublicJWK()).toString();
    }

    private static RSAKey generateKey() {
        try {
            return new RSAKeyGenerator(2048)
                .keyID(KID)
                .algorithm(JWSAlgorithm.RS256)
                .generate();
        } catch (JOSEException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
