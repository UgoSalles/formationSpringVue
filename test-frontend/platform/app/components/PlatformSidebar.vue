<script setup lang="ts">
import type { NavItem } from '../utils/types'

defineProps<{ appName: string; navItems: NavItem[] }>()

// `open` partagé avec le layout (toggle mobile). Tiroir overlay `temporary` (la sidebar desktop est
// un panneau fixe rendu directement par le layout, cf. `.platform-shell__rail`).
const open = defineModel<boolean>('open', { default: false })
</script>

<template>
  <v-navigation-drawer v-model="open" temporary color="background" width="248">
    <PlatformSidebarContent :app-name="appName" :nav-items="navItems" @close="open = false">
      <template #extra>
        <slot name="extra" />
      </template>
    </PlatformSidebarContent>
  </v-navigation-drawer>
</template>
