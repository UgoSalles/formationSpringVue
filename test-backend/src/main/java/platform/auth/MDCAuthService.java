package platform.auth;

import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Adaptateur de l'IdP <strong>MDC</strong> pour la notion d'« utilisateur courant »
 * ({@link IAuthService}). C'est la <strong>seule</strong> classe portant la marque de l'IdP :
 * tout le reste du code ne dépend que de l'abstraction {@link IAuthService}. Pour brancher un
 * autre fournisseur de connexion, il suffit d'exposer une autre implémentation d'{@link IAuthService}
 * (cf. {@link AuthSupportAutoConfiguration}, {@code @ConditionalOnMissingBean}) — en contexte pro,
 * cette classe est renommée {@code MDCAuthService}.
 *
 * <p>Lit le {@code login} de l'utilisateur depuis le {@code SecurityContext} : claim {@code login}
 * du JWT validé, repli sur le {@code sub} ({@link Authentication#getName()}). Renvoie
 * {@link Optional#empty()} quand la requête est anonyme (aucune authentification, ou auth platform
 * désactivée) — le back reste donc fonctionnel sans IdP.
 */
public class MDCAuthService implements IAuthService {

    @Override
    public Optional<String> getLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String login = jwt.getClaimAsString("login");
            if (login != null && !login.isBlank()) {
                return Optional.of(login);
            }
        }
        return Optional.ofNullable(authentication.getName());
    }
}
