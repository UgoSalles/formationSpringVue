<script setup lang="ts">
const store = useNotificationStore()

// IDs cachés localement : le toast disparaît mais la notif reste non-lue dans le store (cloche).
const hidden = ref<Set<string>>(new Set())
const timers = new Map<string, ReturnType<typeof setTimeout>>()

const visible = computed(() =>
  store.notifications.filter((n) => !n.read && !hidden.value.has(n.id)).slice(0, 5),
)

// Auto-dismiss (5 s) : cache le toast sans toucher au compteur de non-lus.
watch(
  visible,
  (list) => {
    for (const n of list) {
      if (!timers.has(n.id)) {
        timers.set(
          n.id,
          setTimeout(() => {
            hidden.value = new Set(hidden.value).add(n.id)
            timers.delete(n.id)
          }, 5000),
        )
      }
    }
  },
  { immediate: true },
)

// Nettoie `hidden` quand une notif est supprimée du store.
watch(
  () => store.notifications,
  (ns) => {
    const ids = new Set(ns.map((n) => n.id))
    const next = new Set([...hidden.value].filter((id) => ids.has(id)))
    if (next.size !== hidden.value.size) hidden.value = next
  },
  { deep: true },
)

// Fermeture manuelle : marque comme lue (décrémente la cloche).
function dismiss(id: string) {
  store.markRead(id)
  const s = new Set(hidden.value)
  s.delete(id)
  hidden.value = s
}

onUnmounted(() => {
  timers.forEach((t) => clearTimeout(t))
  timers.clear()
})
</script>

<template>
  <TransitionGroup name="toast" tag="div" class="platform-toast-container">
    <v-alert
      v-for="n in visible"
      :key="n.id"
      :type="n.level"
      :title="n.title"
      :text="n.message"
      variant="elevated"
      density="comfortable"
      closable
      class="platform-toast"
      @click:close="dismiss(n.id)"
    />
  </TransitionGroup>
</template>
