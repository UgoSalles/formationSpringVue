import { defineStore } from 'pinia'
import type { INotification, NotificationLevel } from '../utils/types'

/**
 * File de notifications in-app (toasts + cloche), portée par Pinia. Remplace l'ancien store Zustand.
 * `unreadCount` est dérivé. Auto-importé (`useNotificationStore`) dans la couche et le projet.
 */
export const useNotificationStore = defineStore('platform-notifications', () => {
  const notifications = ref<INotification[]>([])
  const unreadCount = computed(() => notifications.value.filter((n) => !n.read).length)

  function add(n: { title: string; message?: string; level?: NotificationLevel }) {
    notifications.value.unshift({
      id: crypto.randomUUID(),
      title: n.title,
      message: n.message,
      level: n.level ?? 'info',
      read: false,
      createdAt: new Date(),
    })
  }

  function markRead(id: string) {
    const n = notifications.value.find((x) => x.id === id)
    if (n) n.read = true
  }

  function markAllRead() {
    notifications.value.forEach((n) => {
      n.read = true
    })
  }

  function remove(id: string) {
    notifications.value = notifications.value.filter((n) => n.id !== id)
  }

  function clear() {
    notifications.value = []
  }

  return { notifications, unreadCount, add, markRead, markAllRead, remove, clear }
})
