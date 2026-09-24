import { defineVitestConfig } from '@nuxt/test-utils/config'

export default defineVitestConfig({
  test: {
    environment: 'nuxt',
    environmentOptions: {
      nuxt: { domEnvironment: 'happy-dom' },
    },
  },
  // La couche @platform/front est liée en file: (symlink) en dev/CI : sans ceci, ses imports (ex.
  // @nuxt/test-utils dans @platform/front/testing) seraient résolus depuis son realpath et introuvables.
  // No-op quand la couche est installée depuis le registre (pas de symlink).
  resolve: { preserveSymlinks: true },
})
