package platform.auth;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;

/**
 * Fournisseur d'auteur pour l'audit JPA ({@code @CreatedBy} / {@code @LastModifiedBy} de
 * {@code BaseEntity}).
 *
 * <p>Délègue à {@link IAuthService#getLogin()} : l'audit capte le login courant quel que soit le
 * mécanisme d'authentification derrière l'interface. Repli sur {@value #ANONYMOUS} quand aucun
 * utilisateur n'est authentifié.
 */
public class PlatformAuditorAware implements AuditorAware<String> {

    /** Auteur par défaut quand aucune authentification exploitable n'est disponible. */
    static final String ANONYMOUS = "anonymous";

    private final IAuthService authService;

    /**
     * @param authService service d'accès à l'utilisateur courant
     */
    public PlatformAuditorAware(IAuthService authService) {
        this.authService = authService;
    }

    /**
     * @return le login courant ({@link IAuthService#getLogin()}), ou {@value #ANONYMOUS} si anonyme
     */
    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of(authService.getLogin().orElse(ANONYMOUS));
    }
}
