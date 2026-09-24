// @ts-check
import withNuxt from './.nuxt/eslint.config.mjs'

export default withNuxt({
  rules: {
    'no-restricted-imports': [
      'error',
      {
        patterns: [
          {
            group: ['../**', '..'],
            message: 'Imports relatifs ../ interdits (§5.4) — utiliser les alias @/ ou ~/.',
          },
        ],
      },
    ],
    // Design System (§4.2) : pas de style dans les .vue → tout dans app/assets/css/main.css (.platform-*).
    'vue/no-restricted-block': [
      'error',
      {
        element: 'style',
        message: 'Pas de <style> dans les .vue (§4.2) — déclarer le CSS dans app/assets/css/main.css.',
      },
    ],
    'vue/no-restricted-static-attribute': [
      'error',
      {
        key: 'style',
        message: 'Pas de style= inline (§4.2) — utiliser une classe .platform-* dans main.css.',
      },
    ],
    'vue/no-restricted-v-bind': [
      'error',
      {
        argument: 'style',
        message: 'Pas de :style lié (§4.2) — utiliser une classe .platform-* dans main.css.',
      },
    ],
  },
})
