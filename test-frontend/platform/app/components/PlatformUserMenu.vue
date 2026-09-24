<script setup lang="ts">
import type { UserInfo } from '../utils/types'

defineProps<{ user: UserInfo }>()
const emit = defineEmits<{ logout: [] }>()
</script>

<template>
  <v-menu location="bottom end">
    <template #activator="{ props }">
      <v-btn icon variant="text" size="small" v-bind="props" :aria-label="$t('user.menu')">
        <PlatformUserAvatar :initials="user.initials" size="sm" />
      </v-btn>
    </template>

    <v-list density="compact" nav width="208" border>
      <v-list-item>
        <template #prepend>
          <PlatformUserAvatar :initials="user.initials" size="sm" />
        </template>
        <v-list-item-title class="font-weight-medium">{{ user.name }}</v-list-item-title>
        <v-list-item-subtitle>{{ user.role }}</v-list-item-subtitle>
      </v-list-item>

      <v-divider class="my-1" />

      <v-list-item to="/profile" prepend-icon="mdi-account" :title="$t('user.profile')" />
      <v-list-item to="/settings" prepend-icon="mdi-cog" :title="$t('user.settings')" />

      <v-divider class="my-1" />

      <v-list-item
        prepend-icon="mdi-logout"
        :title="$t('user.logout')"
        base-color="error"
        @click="emit('logout')"
      />
    </v-list>
  </v-menu>
</template>
