import type { NavItem, LegalLink } from './utils/types'
import { themeOptions, defaultThemeName } from './theme/theme.config'

/**
 * Valeurs par défaut du platform, surchargées par l'`app.config.ts` du projet (fusion Nuxt). Le layout
 * et les composants `Platform*` lisent ces données via `useAppConfig().platform`. `user`/`onLogout` viennent
 * de `useAuth()`, pas d'ici.
 */
export default defineAppConfig({
  platform: {
    appName: 'Platform',
    version: '',
    navItems: [] as NavItem[],
    legalLinks: [] as LegalLink[],
    // Thèmes disponibles (source unique theme.config) — alimente le `PlatformThemeSelector`.
    themes: themeOptions,
    // Thème appliqué en dernier recours, quand ni la préférence back (MDC) ni celle du navigateur
    // ne sont disponibles (cf. plugin `03.theme.client`). Surchargeable par le projet ; doit nommer
    // un thème déclaré dans `nuxt.config` (theme.themes).
    defaultTheme: defaultThemeName,
    // TODO(auth): drapeau temporaire. À `false`, la garde globale `auth.global` est court-circuitée →
    // toutes les pages sont accessibles SANS connexion (miroir front du back `platform.auth.enabled=false`
    // + repli permit-all). Prévu pour les projets de démarrage / pro tant qu'aucun IdP n'est branché,
    // afin de laisser tester les CRUD. Repasser à `true` (ou retirer la surcharge projet) une fois l'IdP
    // configuré. Défaut couche = auth activée.
    authEnabled: true,
  },
})
