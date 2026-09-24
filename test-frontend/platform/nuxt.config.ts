import { fileURLToPath } from 'node:url'
import { platformThemes, componentDefaults, defaultThemeName } from './app/theme/theme.config'

// Résolution des chemins de la couche sans dépendre de @nuxt/kit (non résolvable depuis une
// dépendance `file:` / un emplacement hors node_modules du projet).
const resolve = (p: string): string => fileURLToPath(new URL(p, import.meta.url))

// Thèmes, defaults de composants et tokens de design : tout vient de la SOURCE UNIQUE
// `app/theme/theme.config.ts` (épic #1). C'est le seul fichier à éditer pour rebrander l'appli
// (couleurs, defaults, noms de thèmes) — y compris en mode pro où il est inliné verbatim.
const themes = platformThemes

/**
 * Couche Nuxt du platform (`@platform/front`). Un projet l'active via `extends: ['@platform/front']` :
 * il hérite alors du layout, des composants `Platform*`, des composables (auth), du store de
 * notifications (Pinia), du middleware d'auth global, du client HTTP (`$api`), des thèmes
 * Vuetify et de l'i18n de base — sans rien recopier.
 *
 * SPA par défaut (`ssr: false`) : pas de SEO, nginx sert le statique, Spring reste le BFF.
 * Nuxt 4 : le code applicatif de la couche vit dans `app/` (srcDir par défaut) ; `nuxt.config.ts`
 * et `i18n/` restent à la racine de la couche.
 *
 * UI : **Vuetify** (via `vuetify-nuxt-module`) — DaisyUI/Tailwind retirés, pour s'aligner sur la
 * stack pro (Nuxt + Vuetify) et bâtir le facilitateur CRUD sur `v-data-table-server`.
 */
export default defineNuxtConfig({
  ssr: false,

  modules: ['vuetify-nuxt-module', '@pinia/nuxt', '@nuxtjs/i18n', '@innobdx/nuxt-crud'],

  // Facilitateur CRUD (`@innobdx/nuxt-crud`, module Vuetify). Le module scanne
  // `app/domains/<x>/index.ts` du projet (cf. helper `definePlatformDomain`).
  // - `customFetchKey: '$crudApi'` : adaptateur HTTP de la couche (plugin `04.crud-api`) qui réécrit
  //   le GET de liste du module en `POST /search` (pagination serveur platform) ; il s'appuie sur `$api`
  //   (cookies HttpOnly, refresh 401) → auth agnostique de l'IdP.
  // - `locale: 'fr'` : libellés intégrés du module (boutons, dialogue de suppression, recherche,
  //   « aucune donnée »…) en français — défaut du platform. (Bascule dynamique fr/en non câblée côté
  //   module : ses libellés sont résolus une fois, pas réactifs à la locale ; cf. backlog.)
  crud: {
    customFetchKey: '$crudApi',
    locale: 'fr',
    config: {
      // Sous-titres des vues create/update/duplicate du module (rendus par `CrudActionView`).
      subtitle: {
        list: 'Liste',
        create: 'Création',
        update: 'Modification',
        view: 'Consultation',
        duplicate: 'Duplication',
      },
    },
  },

  // Fonts (Inter / JetBrains Mono), transitions de page/toasts, positionnement du conteneur de toasts.
  css: [resolve('./app/assets/css/main.css')],

  vuetify: {
    vuetifyOptions: {
      theme: {
        defaultTheme: defaultThemeName,
        themes,
      },
      icons: { defaultSet: 'mdi' },
      // Seuil « mobile » à md (960px) au lieu du défaut Vuetify lg (1280px) : en dessous, la sidebar
      // passe en tiroir `temporary` (overlay refermé après navigation) ; au-dessus elle reste
      // `permanent`. Évite que des fenêtres < 1280px soient traitées comme mobiles à tort.
      display: { mobileBreakpoint: 'md' },
      // Défauts globaux des composants (registre visuel souverain) — source unique theme.config.
      // Appliqués globalement, hérités par les composants `Platform*` et ceux de `@innobdx/nuxt-crud`.
      defaults: componentDefaults,
    },
  },

  app: {
    head: {
      htmlAttrs: { lang: 'fr' },
    },
    // NB : pas de `pageTransition` ici — Nuxt ne propage pas ce réglage depuis une layer étendue
    // (seul `app.head` fusionne). La transition est déclarée dans le `nuxt.config.ts` du projet
    // généré ; la couche n'en fournit que la CSS (`.page-*` dans app/assets/css/main.css).
  },

  // @nuxtjs/i18n v10 : locales dans `i18n/locales/` (défaut `restructureDir: 'i18n'`, `langDir: 'locales'`),
  // à la racine de la couche (pas dans `app/`). La couche fournit les namespaces de base ; le projet
  // peut en ajouter.
  i18n: {
    strategy: 'no_prefix',
    defaultLocale: 'fr',
    locales: [
      { code: 'fr', files: ['fr/common.json', 'fr/errors.json', 'fr/validation.json'] },
      { code: 'en', files: ['en/common.json', 'en/errors.json', 'en/validation.json'] },
    ],
    detectBrowserLanguage: { useCookie: true, cookieKey: 'platform_locale', redirectOn: 'no prefix' },
  },

  // `AbstractService` est une classe ABSTRAITE : unimport n'auto-scanne pas les `export abstract
  // class` (contrairement aux fonctions/composables/types). On la déclare explicitement pour que les
  // services du projet puissent l'importer via `#imports` (cf. §4.8 du platform).
  imports: {
    imports: [{ name: 'AbstractService', from: resolve('./app/utils/AbstractService') }],
  },

  // Base de l'API (proxifiée sur la même origine). Surchargée par NUXT_PUBLIC_API_BASE.
  runtimeConfig: {
    public: {
      apiBase: '/api',
    },
  },
})
