package platform.auth;

import java.util.Optional;

/**
 * Accès à l'utilisateur authentifié courant, abstrait du mécanisme d'authentification sous-jacent.
 *
 * <p>Permet à tout code (audit JPA, contrôleurs métier…) de récupérer le login courant sans connaître
 * l'IdP derrière : MDC (JWT) en mode standard, autre fournisseur ou aucune auth en mode pro. Un projet
 * qui change de mécanisme n'a qu'à exposer sa propre implémentation de cette interface — tout ce qui
 * dépend du login courant suit automatiquement.
 *
 * <p>Interface justifiée par la convention (§5.1) : système externe à implémentations multiples.
 */
public interface IAuthService {

    /**
     * Login de l'utilisateur courant.
     *
     * @return le login s'il y a un utilisateur authentifié, {@link Optional#empty()} sinon (anonyme)
     */
    Optional<String> getLogin();
}
