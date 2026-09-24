/**
 * Client HTTP du platform (`$api`), basé sur `$fetch`/ofetch. Modèle BFF : les tokens vivent dans des
 * cookies HttpOnly posés par le back (`credentials: 'include'`), jamais lus en JS. Sur 401 (hors
 * `/auth/refresh` lui-même, pour éviter la récursion), on tente un rafraîchissement silencieux puis on
 * rejoue la requête une fois ; en cas d'échec, on renvoie l'utilisateur vers la connexion MDC. Inclut
 * `/auth/me` : au boot, un access token expiré doit déclencher le refresh (cookie valide 7 jours) plutôt
 * que renvoyer directement sur l'IdP — sinon la fermeture du navigateur déconnecte à tort.
 */
export default defineNuxtPlugin(() => {
  const baseURL = useRuntimeConfig().public.apiBase

  const redirectToLogin = () => {
    window.location.href = `${baseURL}/auth/login`
  }

  let refreshing: Promise<boolean> | null = null
  const tryRefresh = (): Promise<boolean> => {
    if (!refreshing) {
      refreshing = $fetch('/auth/refresh', { baseURL, method: 'POST', credentials: 'include' })
        .then(() => true)
        .catch(() => false)
        .finally(() => {
          refreshing = null
        })
    }
    return refreshing
  }

  const base = $fetch.create({
    baseURL,
    credentials: 'include',
    retry: 1,
    retryDelay: 200,
  })

  async function api<T>(request: string, opts: Record<string, unknown> = {}): Promise<T> {
    try {
      return await base<T>(request, opts)
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number }; statusCode?: number })?.response?.status
        ?? (err as { statusCode?: number })?.statusCode
      if (status === 401 && !request.includes('/auth/refresh') && !opts._retry) {
        const refreshed = await tryRefresh()
        if (refreshed) return await base<T>(request, { ...opts, _retry: true })
        redirectToLogin()
      }
      throw err
    }
  }

  return { provide: { api } }
})
