/**
 * Garde d'authentification globale — philosophie platform « fermé par défaut » : toute page est
 * protégée, sauf déclaration explicite `definePageMeta({ public: true })`. Remplace l'ancien
 * composant `ProtectedRoute`. L'identité est déjà résolue (plugin `02.auth.client`) quand ce
 * middleware s'exécute ; si l'utilisateur est anonyme, on le renvoie vers la connexion MDC.
 */
export default defineNuxtRouteMiddleware((to) => {
  if (to.meta.public) return

  // TODO(auth): auth désactivée (projet de démarrage / pro sans IdP) → aucune garde, toutes les pages
  // sont accessibles pour tester les CRUD sans connexion. Miroir du back `platform.auth.enabled=false`.
  // Voir `platform.authEnabled` dans `app.config.ts` ; à retirer/repasser à true une fois l'IdP branché.
  const { platform } = useAppConfig()
  if (platform?.authEnabled === false) return

  const { isAuthenticated, login } = useAuth()
  if (!isAuthenticated.value) {
    login()
    return abortNavigation()
  }
})
