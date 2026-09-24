export default defineAppConfig({
  platform: {
    appName: 'test',
    version: '0.1.0',
    // TODO(auth): aucun IdP branché → toutes les pages sont accessibles SANS connexion (test des
    // CRUD). Pour activer l'auth : renseigner AUTH_* dans .env, passer platform.auth.enabled=true côté
    // back, puis repasser ceci à true (ou supprimer la ligne pour hériter du défaut couche = true).
    authEnabled: false,
    // Thème par défaut hérité de la couche (source unique `app/theme/theme.config.ts`). Pour le
    // surcharger, décommenter et nommer un thème déclaré dans theme.config :
    // defaultTheme: 'souverainLight',
    // Libellés = clés i18n (cf. i18n/ de la couche). `icon` = nom d'icône MDI (rendu par Vuetify).
    navItems: [
      { label: 'nav.home', to: '/', icon: 'mdi-home' },
      { label: 'nav.regions', to: '/regions', icon: 'mdi-map' },
      { label: 'nav.pokemons', to: '/pokemons', icon: 'mdi-paw' },
    ],
    // Liens légaux affichés dans le footer (pages publiques générées sous app/pages/).
    // Contenu des pages à compléter côté projet.
    legalLinks: [
      { label: 'legal.mentions', to: '/mentions-legales' },
      { label: 'legal.privacy', to: '/confidentialite' },
      { label: 'legal.cookies', to: '/cookies' },
      { label: 'legal.contact', to: '/contact' },
    ],
  },
})
