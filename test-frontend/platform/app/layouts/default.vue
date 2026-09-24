<script setup lang="ts">
import type { UserInfo } from '../utils/types'

// Layout par défaut du platform — shell « panneaux flottants » : la sidebar et la colonne de contenu
// sont des surfaces arrondies posées sur un fond teinté (cf. `.platform-shell*` dans main.css).
// Sur desktop la sidebar est un panneau fixe ; sous le breakpoint `mobile` (md), elle bascule en
// tiroir overlay (`PlatformSidebar`). Données d'app via useAppConfig().platform ; identité via useAuth().
const { platform } = useAppConfig()
const { user, logout } = useAuth()
const { mobile } = useDisplay()
const route = useRoute()

const drawer = ref(false)

// Pages enveloppées dans un panneau (surface arrondie pleine hauteur), pour la cohérence du shell.
// Une page peut s'en exclure (contenu qui porte ses propres cartes, ex. dashboard bento) via
// `definePageMeta({ bare: true })`.
const barePage = computed(() => route.meta.bare === true)

const userInfo = computed<UserInfo | undefined>(() => {
  if (!user.value) return undefined
  const display = user.value.login || user.value.email || user.value.id || '?'
  return {
    name: display,
    role: user.value.role,
    initials: display.slice(0, 2).toUpperCase(),
  }
})
</script>

<template>
  <v-app>
    <div class="platform-shell">
      <!-- Sidebar : panneau fixe (desktop) -->
      <aside v-if="!mobile" class="platform-shell__rail">
        <PlatformSidebarContent :app-name="platform.appName" :nav-items="platform.navItems" />
      </aside>

      <!-- Sidebar : tiroir overlay (mobile) -->
      <PlatformSidebar
        v-else
        v-model:open="drawer"
        :app-name="platform.appName"
        :nav-items="platform.navItems"
      />

      <div class="platform-shell__body">
        <PlatformHeader
          class="platform-panel"
          :user="userInfo"
          @menu-open="drawer = !drawer"
          @logout="logout"
        />

        <main class="platform-shell__content">
          <div :class="{ 'platform-page-panel': !barePage }">
            <slot />
          </div>
        </main>

        <PlatformFooter :version="platform.version" :legal-links="platform.legalLinks" />
      </div>
    </div>

    <PlatformToastContainer />
  </v-app>
</template>
