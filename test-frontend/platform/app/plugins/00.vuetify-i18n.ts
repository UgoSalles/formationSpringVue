import { en, fr } from 'vuetify/locale'

/**
 * Pont i18n Vuetify ↔ vue-i18n. `vuetify-nuxt-module` détecte `@nuxtjs/i18n` et délègue toute la
 * traduction de Vuetify à vue-i18n : les composants appellent `t('$vuetify.<clé>')` (footer du
 * data-table, « éléments par page », pagination, etc.). Sans ces clés dans vue-i18n, la clé brute
 * s'affiche (`$vuetify.dataFooter.itemsPerPageText`).
 *
 * On injecte donc les paquets de traduction officiels de Vuetify (`vuetify/locale`) sous la clé
 * `$vuetify`, pour chaque locale du platform. `mergeLocaleMessage` met à jour les messages réactifs lus
 * par l'adaptateur du module → traduction correcte, et réactive au changement de langue.
 *
 * `dependsOn: ['i18n:plugin']` garantit que vue-i18n (`$i18n`) est initialisé avant le merge — c'est
 * le nom de plugin sur lequel `vuetify-nuxt-module` se synchronise lui-même.
 */
export default defineNuxtPlugin({
  name: 'platform:vuetify-i18n',
  dependsOn: ['i18n:plugin'],
  setup(nuxtApp) {
    const i18n = nuxtApp.$i18n as
      | { mergeLocaleMessage?: (locale: string, messages: Record<string, unknown>) => void }
      | undefined
    if (!i18n?.mergeLocaleMessage) {
      return
    }
    i18n.mergeLocaleMessage('fr', { $vuetify: fr })
    i18n.mergeLocaleMessage('en', { $vuetify: en })
  },
})
