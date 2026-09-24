<script setup lang="ts">
import type { INotification } from '../utils/types'

const store = useNotificationStore()

const LEVEL_COLOR: Record<INotification['level'], string> = {
  info: 'info',
  success: 'success',
  warning: 'warning',
  error: 'error',
}

function timeAgo(date: Date): string {
  const diff = Math.floor((Date.now() - new Date(date).getTime()) / 1000)
  if (diff < 60) return `${diff}s`
  if (diff < 3600) return `${Math.floor(diff / 60)}min`
  return `${Math.floor(diff / 3600)}h`
}
</script>

<template>
  <div class="d-flex flex-column platform-notif-panel">
    <div class="d-flex align-center justify-space-between px-3 py-2 border-b">
      <span class="text-body-2 font-weight-bold">
        {{ $t('notifications.title') }}
        <v-chip v-if="store.unreadCount > 0" size="x-small" color="secondary" class="ml-1">
          {{ store.unreadCount }}
        </v-chip>
      </span>
      <div class="d-flex ga-1">
        <v-btn
          v-if="store.unreadCount > 0"
          icon="mdi-check-all"
          variant="text"
          size="x-small"
          :aria-label="$t('notifications.markAllRead')"
          @click="store.markAllRead()"
        />
        <v-btn
          v-if="store.notifications.length > 0"
          icon="mdi-delete"
          variant="text"
          size="x-small"
          color="error"
          :aria-label="$t('notifications.clearAll')"
          @click="store.clear()"
        />
      </div>
    </div>

    <div class="flex-grow-1 overflow-y-auto">
      <div
        v-if="store.notifications.length === 0"
        class="px-3 py-6 text-center text-body-2 platform-muted"
      >
        {{ $t('notifications.empty') }}
      </div>
      <template v-else>
        <div
          v-for="n in store.notifications"
          :key="n.id"
          class="d-flex ga-2 px-3 py-2 border-b platform-notif"
          :class="{ 'platform-notif--read': n.read }"
          @click="store.markRead(n.id)"
        >
          <v-icon icon="mdi-circle" :color="LEVEL_COLOR[n.level]" size="10" class="mt-1 flex-shrink-0" />
          <div class="flex-grow-1 text-truncate">
            <div class="text-body-2 font-weight-medium">{{ n.title }}</div>
            <div v-if="n.message" class="text-caption platform-muted">{{ n.message }}</div>
          </div>
          <div class="d-flex flex-column align-end ga-1 flex-shrink-0">
            <span class="text-caption platform-subtle">{{ timeAgo(n.createdAt) }}</span>
            <v-btn
              icon="mdi-delete"
              variant="text"
              size="x-small"
              :aria-label="$t('notifications.remove')"
              @click.stop="store.remove(n.id)"
            />
          </div>
        </div>
      </template>
    </div>
  </div>
</template>
