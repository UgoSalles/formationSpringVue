/**
 * Middleware global « page introuvable » : ramène à `/` toute URL qui ne correspond à aucune page
 * réelle, et signale l'événement au backend (`POST /logs/client`). Deux cas couverts :
 *  - **404 générique** : la route attrape-tout `[...slug].vue` (nom de route `slug`) ;
 *  - **entité CRUD inexistante** : `@innobdx/nuxt-crud` enregistre des routes `/:entity` et
 *    `/:entity/:action/:id?` pour tout segment ; si l'entité ne correspond à aucun domaine déclaré
 *    (`nuxtApp.$crudDomains`, keyé par `endpoint` = segment d'URL), c'est une 404.
 *
 * S'exécute après `auth.global` (ordre alphabétique) : un utilisateur anonyme est déjà renvoyé vers
 * la connexion en amont. Le signalement au back est **non bloquant** (la redirection n'attend pas).
 */
const CRUD_ROUTE_NAMES = ['crud-entity', 'crud-entity-action']
const SLUG_ROUTE_NAME = 'slug'

export default defineNuxtRouteMiddleware((to) => {
  const nuxtApp = useNuxtApp()
  const routeName = typeof to.name === 'string' ? to.name : ''
  const entity = typeof to.params.entity === 'string' ? to.params.entity : ''

  const isUnknownCrudEntity =
    CRUD_ROUTE_NAMES.includes(routeName) && entity !== '' && !(entity in (nuxtApp.$crudDomains ?? {}))
  const isGenericNotFound = routeName === SLUG_ROUTE_NAME

  if (!isUnknownCrudEntity && !isGenericNotFound) {
    return
  }

  // Signalement non bloquant : on ne retarde pas la redirection si le back est lent/indisponible.
  void nuxtApp
    .$api('/logs/client', {
      method: 'POST',
      body: { level: 'warn', message: 'route introuvable', context: { path: to.fullPath } },
    })
    .catch(() => {})

  return navigateTo('/')
})
