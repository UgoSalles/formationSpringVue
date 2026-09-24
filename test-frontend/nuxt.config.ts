import { fileURLToPath } from 'node:url'

// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  extends: ['./platform'],
  alias: { '@platform/front': fileURLToPath(new URL('./platform', import.meta.url)) },

  // SPA : pas de SEO, nginx sert le statique, Spring reste le BFF (cf. platform).
  ssr: false,

  compatibilityDate: '2025-07-15',

  modules: ['@nuxt/eslint', '@vite-pwa/nuxt'],

  // Serveur de dev derrière un reverse proxy (Traefik) sur un domaine : Vite refuse par défaut les
  // hosts non listés (protection anti DNS-rebinding). On les autorise — concerne UNIQUEMENT le
  // serveur de dev (en prod, le front est un build statique servi par nginx, pas de serveur Vite).
  // Restreindre à des domaines précis si besoin : `allowedHosts: ['.mon-domaine.fr']`.
  vite: {
    server: {
      allowedHosts: true,
    },
  },

  // Dev standalone : proxie /api vers le back local (localhost:8080 = APP_PORT) — same-origin, pas de CORS.
  // NB : le devProxy de Nitro retire le préfixe `/api` avant de transmettre la requête (h3 `app.use`
  // mount-path) — on le réintègre dans le `target` pour retomber sur le context-path `/api` du back.
  nitro: {
    devProxy: {
      '/api': { target: 'http://localhost:8080/api', changeOrigin: true },
    },
  },

  // Namespaces i18n du projet (fusionnés avec ceux de la couche @platform/front).
  i18n: {
    locales: [
      { code: 'fr', files: ['fr/regions.json', 'fr/pokemons.json', 'fr/nav.json'] },
    ],
  },

  app: {
    head: {
      title: 'test',
    },
    // Transition de page native (CSS `.page-*` fournie par la couche @platform/front).
    // Déclarée ici et non dans la couche : Nuxt ne propage pas `pageTransition` depuis une layer
    // étendue (contrairement à `app.head` qui, lui, fusionne) — elle doit vivre dans le projet.
    pageTransition: { name: 'page', mode: 'out-in' },
  },

  // PWA (installable). Cache : assets -> précache ; API -> NetworkFirst ; jamais les tokens (cookies).
  pwa: {
    registerType: 'autoUpdate',
    manifest: {
      name: 'test',
      short_name: 'test',
      theme_color: '#003399',
      background_color: '#2C3040',
      display: 'fullscreen',
      start_url: '/',
      // Ajouter les icônes dans public/icons/ puis les déclarer ici (192x192, 512x512).
      icons: [],
    },
    workbox: {
      navigateFallback: '/',
      runtimeCaching: [
        {
          urlPattern: /\/api\/.*/i,
          handler: 'NetworkFirst',
          options: { cacheName: 'api-cache' },
        },
      ],
    },
    // Service worker désactivé en dev : en SPA il n'apporte rien (pas de mode hors ligne, cf. platform)
    // et son enregistrement + interception des navigations alourdit/perturbe le dev server. Le SW
    // est généré et testable au build (nuxt generate). Passer à true ponctuellement pour le tester.
    devOptions: { enabled: false },
  },
})
