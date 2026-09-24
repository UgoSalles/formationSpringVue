/**
 * Applique les préférences utilisateur au démarrage (client/SPA) : **langue** (vue-i18n) et **thème**
 * (Vuetify).
 *
 * Source des préférences, par priorité décroissante :
 *   1. **préférence back** — MDC via `GET /api/auth/preferences` (`{ langue, theme }`). Étape
 *      retirée en contexte pro (MDC absent) ; gracieuse (toute absence/erreur/401 → on poursuit).
 *   2. **préférence navigateur** — pour le thème, `prefers-color-scheme` mappé sur un thème platform
 *      sombre/clair ; pour la langue, la détection de `@nuxtjs/i18n` (cookie/navigateur) fait foi.
 *   3. **défaut** — `useAppConfig().platform.defaultTheme` (thème) ; `defaultLocale` (langue).
 *
 * Thème : on ne peut pas appeler `useTheme()` ici (exige un setup de composant). On s'abonne au hook
 * `vuetify:ready` de `vuetify-nuxt-module`, qui fournit l'instance Vuetify (`vuetify.theme`). Langue :
 * appliquée dès que la préférence back est connue, via `setLocale` de vue-i18n.
 *
 * Ordre : ce plugin (03) s'exécute après `02.auth.client` (identité résolue) ; le hook thème se
 * déclenche quand Vuetify est prêt.
 */
import { resolveThemeName } from '../theme/theme.config'

// Thèmes intégrés de Vuetify, toujours présents en plus des thèmes platform — à ne pas sélectionner.
const VUETIFY_BUILTIN_THEMES = new Set(['light', 'dark'])

interface UserPreferences {
  langue?: string
  theme?: string
}

export default defineNuxtPlugin((nuxtApp) => {
  const { isAuthenticated } = useAuth()
  const defaultTheme = useAppConfig().platform.defaultTheme
  const { $api } = nuxtApp

  // Une seule requête de préférences, partagée par l'application langue (immédiate) et thème (hook).
  const prefsPromise: Promise<UserPreferences | null> = isAuthenticated.value
    ? $api<UserPreferences>('/auth/preferences').catch(() => null)
    : Promise.resolve(null)

  // ── Langue : applique la préférence back si c'est une locale connue de vue-i18n ───────────────
  prefsPromise.then((prefs) => {
    const langue = prefs?.langue
    if (!langue) {
      return
    }
    const i18n = nuxtApp.$i18n as
      | {
          locale?: { value?: string }
          locales?: { value?: { code: string }[] }
          setLocale?: (code: string) => unknown
        }
      | undefined
    const known = i18n?.locales?.value?.some((l) => l.code === langue) ?? false
    // Ne bascule que si nécessaire : un setLocale vers la locale déjà active peut re-déclencher
    // une navigation/rendu inutile.
    if (known && i18n?.setLocale && i18n.locale?.value !== langue) {
      i18n.setLocale(langue)
    }
  })

  // ── Thème : appliqué quand Vuetify est prêt ──────────────────────────────────────────────────
  nuxtApp.hook('vuetify:ready', async (vuetify) => {
    const theme = vuetify.theme
    const available = theme.themes.value
    const isKnown = (name?: string | null): name is string => !!name && name in available

    const apply = (name?: string | null): boolean => {
      if (!isKnown(name)) {
        return false
      }
      theme.change(name)
      return true
    }

    // 1. Préférence back (MDC). Gracieux : toute absence/erreur poursuit la chaîne. `resolveThemeName`
    // mappe les anciens noms (ex. `platform-light`) vers les noms actuels — les préférences déjà
    // stockées côté MDC restent honorées après un renommage de thème.
    const prefs = await prefsPromise
    if (apply(resolveThemeName(prefs?.theme))) {
      return
    }

    // 2. Préférence du navigateur, mappée sur un thème platform sombre/clair existant.
    // ⚠️ Vuetify enregistre TOUJOURS ses thèmes intégrés `light`/`dark` (primary #1867C0, secondary
    // teal #48A9A6) EN PLUS des nôtres, et en premier dans la map. On les exclut, sinon `find`
    // renverrait un thème Vuetify par défaut au lieu d'un thème platform (mauvaises couleurs).
    if (window.matchMedia) {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      const match = Object.entries(available)
        .find(([name, t]) => !VUETIFY_BUILTIN_THEMES.has(name) && Boolean(t.dark) === prefersDark)?.[0]
      if (apply(match)) {
        return
      }
    }

    // 3. Thème par défaut configurable.
    apply(defaultTheme)
  })
})
