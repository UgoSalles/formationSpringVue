/**
 * Helpers de test du platform — sous-chemin `@platform/front/testing`.
 *
 * Objectif : des tests front faciles à écrire et SANS backend ni MDC réels. On ne teste que le
 * front ; la frontière HTTP (`$api` → endpoints BFF) et l'identité sont mockées. Import unique :
 *
 *   import { mockApi, loginAs, buildUser, expectToast, renderSuspended, screen } from '@platform/front/testing'
 *
 * Les trois niveaux de test du platform :
 *   - unitaire      : fonctions / erreurs pures (vitest seul — ces helpers ne sont pas nécessaires ;
 *                     mettre `// @vitest-environment node` en tête pour s'affranchir de Nuxt).
 *   - intégration   : services contre `mockApi({...})` — vérifie les appels API, backend stubé.
 *   - fonctionnement: pages / composants montés via `renderSuspended` / `mountSuspended`, `$api`
 *                     stubé, identité via `loginAs()` — assertions DOM + `expectToast`.
 */
import { registerEndpoint, mountSuspended, renderSuspended } from '@nuxt/test-utils/runtime'
import { screen, fireEvent, waitFor } from '@testing-library/vue'
import { expect } from 'vitest'
import { useNotificationStore } from '#imports'
import type { IAuthUser, NotificationLevel } from '#imports'

// Import unique : on ré-expose les primitives de test les plus courantes.
export { mountSuspended, renderSuspended, screen, fireEvent, waitFor }

/** Fixture d'utilisateur authentifié, surchargeable champ par champ. */
export function buildUser(overrides: Partial<IAuthUser> = {}): IAuthUser {
  return {
    id: '01HZ000000000000000000USER',
    login: 'tester',
    email: 'tester@example.com',
    role: 'USER',
    ...overrides,
  }
}

/** Réponse mockée : valeur directe (sérialisée en JSON) ou fonction `(event) => valeur`. */
type MockHandler = unknown | ((event: unknown) => unknown)

/**
 * Stube des endpoints BFF (`/api/**`). Clé = `"<MÉTHODE> <chemin>"` (ex. `"POST /examples/search"`)
 * ou simplement `"<chemin>"` (GET par défaut). Le préfixe `/api` est ajouté s'il manque. À appeler
 * dans un `beforeEach` ou en début de test, avant le rendu / l'appel.
 */
export function mockApi(routes: Record<string, MockHandler>): void {
  for (const [key, value] of Object.entries(routes)) {
    const parts = key.trim().split(/\s+/)
    const method = parts.length > 1 ? parts[0] : 'GET'
    const rawPath = parts.length > 1 ? parts[1] : parts[0]
    const path = rawPath.startsWith('/api') ? rawPath : `/api${rawPath}`
    const handler = typeof value === 'function' ? (value as (e: unknown) => unknown) : () => value
    registerEndpoint(path, { method: method.toUpperCase() as never, handler: handler as never })
  }
}

/**
 * Stube l'identité courante en mockant `GET /api/auth/me` (le plugin d'auth la charge au boot).
 * `loginAs()` → utilisateur par défaut · `loginAs(buildUser({ role: 'ADMIN' }))` → custom ·
 * `loginAs(null)` → anonyme.
 */
export function loginAs(user: IAuthUser | null = buildUser()): void {
  mockApi({ 'GET /auth/me': user })
}

/**
 * Vérifie qu'un toast a été émis (store Pinia de notifications). Filtres optionnels : niveau et/ou
 * fragment de titre.
 */
export function expectToast(level?: NotificationLevel, titleIncludes?: string): void {
  const { notifications } = useNotificationStore()
  const found = notifications.some(
    (n) =>
      (level === undefined || n.level === level) &&
      (titleIncludes === undefined || n.title.includes(titleIncludes)),
  )
  expect(
    found,
    `toast attendu (niveau=${level ?? 'tous'}, titre~="${titleIncludes ?? ''}")`,
  ).toBe(true)
}
