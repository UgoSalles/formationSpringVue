package platform.test;

import com.nimbusds.jwt.JWTClaimsSet;
import java.time.Instant;
import java.util.Date;

/**
 * Fabrique de tokens d'accès de test, signés en RS256 par {@link MDCMock}. Reproduit les claims émis
 * par MDC : {@code sub}, {@code login}, {@code email}, {@code role}. Le claim {@code role} est mappé
 * par le platform en autorité {@code ROLE_<role>}.
 */
public final class TestTokens {

    private static final String DEFAULT_SUB = "01TEST00000000000000000000";

    private TestTokens() {
    }

    /** Token d'un utilisateur authentifié avec le rôle {@code USER}. */
    public static String user() {
        return withRole("USER");
    }

    /** Token d'un utilisateur authentifié avec le rôle {@code ADMIN}. */
    public static String admin() {
        return withRole("ADMIN");
    }

    /** Token valide portant le rôle indiqué. */
    public static String withRole(String role) {
        return token(DEFAULT_SUB, "tester", "tester@platform.test", role, Instant.now().plusSeconds(3600));
    }

    /** Token déjà expiré (rejeté par la validation du timestamp → 401). */
    public static String expired(String role) {
        return token(DEFAULT_SUB, "tester", "tester@platform.test", role, Instant.now().minusSeconds(60));
    }

    /** Token {@code USER} portant un claim {@code plan}. */
    public static String withPlan(String plan) {
        return withRoleAndPlan("USER", plan);
    }

    /** Token valide portant le rôle et le claim {@code plan} indiqués. */
    public static String withRoleAndPlan(String role, String plan) {
        return token(DEFAULT_SUB, "tester", "tester@platform.test", role, plan, Instant.now().plusSeconds(3600));
    }

    /** Construit et signe un token avec tous les claims explicites (sans plan). */
    public static String token(String sub, String login, String email, String role, Instant expiresAt) {
        return token(sub, login, email, role, null, expiresAt);
    }

    /** Construit et signe un token avec tous les claims explicites, claim {@code plan} optionnel. */
    public static String token(
            String sub, String login, String email, String role, String plan, Instant expiresAt) {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
            .subject(sub)
            .claim("login", login)
            .claim("email", email)
            .claim("role", role)
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(expiresAt));
        if (plan != null) {
            builder.claim("plan", plan);
        }
        return MDCMock.sign(builder.build());
    }
}
