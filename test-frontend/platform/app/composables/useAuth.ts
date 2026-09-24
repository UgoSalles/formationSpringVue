import type { IAuthUser } from '../utils/types'

/**
 * Accès à l'authentification (modèle BFF). État partagé via `useState`. Charge l'identité courante
 * via `GET /api/auth/me` ; `login()`/`logout()` redirigent vers le back (qui gère le flux MDC).
 * Remplace l'ancien `AuthProvider`/`useAuth` React. L'initialisation au boot est faite par le
 * plugin `02.auth.client.ts`.
 */
export function useAuth() {
  const user = useState<IAuthUser | null>('platform-auth-user', () => null)
  const isLoading = useState<boolean>('platform-auth-loading', () => true)

  const baseURL = useRuntimeConfig().public.apiBase
  const { $api } = useNuxtApp()

  /** Recharge l'identité courante depuis le back (`null` si non authentifié). */
  const reload = async (): Promise<void> => {
    isLoading.value = true
    try {
      user.value = await $api<IAuthUser>('/auth/me')
    } catch {
      user.value = null
    } finally {
      isLoading.value = false
    }
  }

  /** Redirige vers la connexion MDC (via le back). */
  const login = (): void => {
    window.location.href = `${baseURL}/auth/login`
  }

  /** Déconnecte (révocation côté back) puis renvoie vers la connexion. */
  const logout = async (): Promise<void> => {
    try {
      await $api('/auth/logout', { method: 'POST' })
    } finally {
      window.location.href = `${baseURL}/auth/login`
    }
  }

  return {
    user,
    isAuthenticated: computed(() => user.value !== null),
    isLoading,
    login,
    logout,
    reload,
  }
}
