/**
 * Source UNIQUE des thèmes du platform (épic #1 — centralisation).
 *
 * Tout ce qui touche à l'identité visuelle vit ici : palettes des thèmes, defaults de composants
 * Vuetify, tokens de design non-couleur, noms de thèmes. Les autres fichiers ne font que *consommer*
 * ce module :
 *   - `nuxt.config.ts`  → `platformThemes`, `componentDefaults`, `defaultThemeName` (build Vuetify) ;
 *   - `app.config.ts`   → `themeOptions`, `defaultThemeName` (exposés en `useAppConfig().platform`) ;
 *   - `PlatformThemeSelector.vue` → `useAppConfig().platform.themes` (liste du sélecteur) ;
 *   - `plugins/03.theme.client.ts` → `useAppConfig().platform.defaultTheme` (résolution au boot).
 *
 * ► PERSONNALISATION (perso & mode pro) : ce fichier est LE point d'entrée pour rebrander l'appli.
 *   En mode pro il est inliné verbatim dans le projet — changer les couleurs/noms ici suffit, sans
 *   toucher au reste de la couche. Ajouter/retirer un thème = éditer `platformThemes` + `themeOptions`
 *   (mêmes clés). Aucun nom de thème n'est codé en dur ailleurs.
 *
 * Couleurs : clés Vuetify standard (`background`, `surface`, `primary`, `on-*`, `surface-variant`…).
 * Variables : opacités d'emphase Vuetify (`high/medium/disabled-emphasis-opacity`, `border-*`) PLUS
 * les tokens platform `--v-platform-*` (espacements, rayons, transitions) consommés par `global.css`.
 */

/** Tokens de design NON-couleur, partagés par tous les thèmes (exposés en `var(--v-platform-*)`). */
const designTokens = {
  'platform-space-xs': '0.25rem',
  'platform-space-sm': '0.5rem',
  'platform-space-md': '1rem',
  'platform-space-lg': '1.5rem',
  'platform-space-xl': '2rem',
  'platform-radius-sm': '0.375rem',
  'platform-radius-md': '0.625rem',
  'platform-radius-lg': '0.875rem',
  'platform-emphasis-muted': '0.6',
  'platform-emphasis-subtle': '0.4',
  'platform-emphasis-faint': '0.5',
  'platform-transition-fast': '0.15s',
}

/** Nom du thème appliqué par défaut (clair). Doit exister dans `platformThemes`. */
export const defaultThemeName = 'platform-light'

/** Les thèmes Vuetify du platform — palette « souverain » (bleu régalien + or). */
export const platformThemes = {
  // ── Clair (défaut) ────────────────────────────────────────────────────────────────────────
  'platform-light': {
    dark: false,
    colors: {
      'background': '#F7F8FB',
      'surface': '#FFFFFF',
      'surface-variant': '#EEF1F6',
      'on-surface-variant': '#64708A',
      'primary': '#1F3C88',
      'primary-darken-1': '#162C66',
      'secondary': '#5B6B8C',
      'accent': '#C9A227',
      'info': '#2D5BD0',
      'success': '#2E7D5B',
      'warning': '#C9871F',
      'error': '#C0392B',
      'on-primary': '#FFFFFF',
      'on-secondary': '#FFFFFF',
      'on-accent': '#332806',
      'on-background': '#14213D',
      'on-surface': '#14213D',
    },
    variables: {
      ...designTokens,
      'border-color': '#3A4868',
      'border-opacity': 0.16,
      'high-emphasis-opacity': 0.92,
      'medium-emphasis-opacity': 0.62,
      'disabled-opacity': 0.38,
    },
  },

  // ── Sombre ────────────────────────────────────────────────────────────────────────────────
  'platform-dark': {
    dark: true,
    colors: {
      'background': '#0E1626',
      'surface': '#16223C',
      'surface-variant': '#25324F',
      'on-surface-variant': '#9AA7C2',
      'primary': '#6E93F0',
      'primary-darken-1': '#3F66C4',
      'secondary': '#8A99B8',
      'accent': '#E0BC4A',
      'info': '#6E93F0',
      'success': '#5ECA97',
      'warning': '#E0A53A',
      'error': '#F06A6A',
      'on-primary': '#0A142B',
      'on-secondary': '#0A142B',
      'on-accent': '#2A2206',
      'on-background': '#E8ECF5',
      'on-surface': '#E8ECF5',
    },
    variables: {
      ...designTokens,
      'border-color': '#C5D2EC',
      'border-opacity': 0.14,
      'high-emphasis-opacity': 0.94,
      'medium-emphasis-opacity': 0.66,
      'disabled-opacity': 0.4,
    },
  },

  // ── Accessibilité (WCAG AAA, haut-contraste, dérivé de la palette souverain) ────────────────
  'platform-a11y': {
    dark: false,
    colors: {
      'background': '#FFFFFF',
      'surface': '#FFFFFF',
      'surface-variant': '#EAEDF3',
      'on-surface-variant': '#2A3142',
      'primary': '#122861',
      'primary-darken-1': '#0B193F',
      'secondary': '#2C374D',
      'accent': '#5A4500',
      'info': '#0E3A9B',
      'success': '#0A5A39',
      'warning': '#5E3D00',
      'error': '#971B12',
      'on-primary': '#FFFFFF',
      'on-secondary': '#FFFFFF',
      'on-accent': '#FFFFFF',
      'on-background': '#000000',
      'on-surface': '#000000',
    },
    variables: {
      ...designTokens,
      'border-color': '#000000',
      'border-opacity': 0.5,
      'high-emphasis-opacity': 1,
      'medium-emphasis-opacity': 0.82,
      'disabled-opacity': 0.5,
    },
  },
}

/**
 * Defaults globaux des composants Vuetify — registre visuel du platform : boutons plats arrondis
 * sans majuscules, cartes bordées plates, champs outlined, chips pilule, alertes tonales.
 * Hérités par les composants `Platform*` ET par ceux de `@innobdx/nuxt-crud` (qui ne les surchargent
 * pas). `style` inline ici (et non dans un `.vue`) reste conforme : c'est de la config, pas un composant.
 */
export const componentDefaults = {
  VBtn: {
    rounded: 'lg',
    flat: true,
    density: 'compact',
    style: 'text-transform:none;letter-spacing:0;font-weight:500;font-size:0.8125rem',
  },
  VCard: { rounded: 'lg', elevation: 0, border: true },
  VTextField: { variant: 'outlined', density: 'comfortable', rounded: 'lg' },
  VSelect: { variant: 'outlined', density: 'comfortable', rounded: 'lg' },
  VChip: { rounded: 'pill' },
  VAlert: { variant: 'tonal', rounded: 'lg' },
}

/**
 * Anciens noms de thèmes → noms actuels. Préserve les préférences utilisateur DÉJÀ stockées côté MDC
 * après un renommage : une préférence `souverainLight` continue d'appliquer le thème clair au lieu
 * d'être ignorée. Étendre cette table à chaque renommage de thème.
 */
export const legacyThemeAliases: Record<string, string> = {
  souverainDark: 'platform-dark',
  souverainLight: 'platform-light',
  souverainA11y: 'platform-a11y',
}

/** Normalise un nom de thème : applique l'alias legacy s'il existe, sinon renvoie le nom tel quel. */
export function resolveThemeName(name?: string | null): string | undefined {
  if (!name) return undefined
  return legacyThemeAliases[name] ?? name
}

export interface ThemeOption {
  value: string
  /** Libellé court affiché dans le sélecteur (clé i18n résolue côté composant, sinon littéral). */
  label: string
}

/** Options du sélecteur de thème (ordre d'affichage). Clés = celles de `platformThemes`. */
export const themeOptions: ThemeOption[] = [
  { value: 'platform-light', label: 'theme.light' },
  { value: 'platform-dark', label: 'theme.dark' },
  { value: 'platform-a11y', label: 'theme.a11y' },
]
