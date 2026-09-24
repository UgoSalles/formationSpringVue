<script setup lang="ts">
import type { UserInfo } from '../utils/types'

defineProps<{ user?: UserInfo }>()
const emit = defineEmits<{ 'menu-open': []; logout: [] }>()
const { mobile } = useDisplay()

const notifications = useNotificationStore()
</script>

<template>
  <header class="platform-header">
    <v-app-bar-nav-icon
      v-if="mobile"
      :aria-label="$t('actions.openMenu')"
      @click="emit('menu-open')"
    />

    <!-- Pas de recherche globale ici : une recherche appartient aux pages/tableaux (cf. `searchField`
         du facilitateur CRUD), pas au header présent sur toutes les pages. -->
    <v-spacer />

    <PlatformLanguageSelector class="d-none d-sm-block" />
    <PlatformThemeSelector class="d-none d-sm-block" />

    <v-menu :close-on-content-click="false" location="bottom end">
      <template #activator="{ props }">
        <v-btn
          icon
          variant="text"
          size="small"
          v-bind="props"
          :aria-label="$t('notifications.title')"
        >
          <v-badge
            :model-value="notifications.unreadCount > 0"
            :content="notifications.unreadCount > 99 ? '99+' : notifications.unreadCount"
            color="error"
          >
            <v-icon icon="mdi-bell-outline" />
          </v-badge>
        </v-btn>
      </template>
      <v-card width="320" border>
        <PlatformNotificationPanel />
      </v-card>
    </v-menu>

    <PlatformUserMenu v-if="user" :user="user" @logout="emit('logout')" />
  </header>
</template>
