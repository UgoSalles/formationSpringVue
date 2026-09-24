package platform.auth;

/**
 * Tokens retournés par l'IdP lors d'un échange de code ou d'un rafraîchissement.
 *
 * @param accessToken  JWT d'accès (RS256)
 * @param refreshToken token de rafraîchissement opaque
 * @param expiresIn    durée de vie de l'access token, en secondes
 */
public record AuthTokens(String accessToken, String refreshToken, long expiresIn) {}
