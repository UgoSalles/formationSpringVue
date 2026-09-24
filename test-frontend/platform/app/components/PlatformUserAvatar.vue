<script setup lang="ts">
const props = withDefaults(defineProps<{ initials: string; size?: 'sm' | 'md' }>(), {
  size: 'md',
})

const avatarSize = computed(() => (props.size === 'sm' ? 28 : 36))
</script>

<template>
  <v-avatar :size="avatarSize" color="secondary" class="font-weight-bold">
    <span :class="size === 'sm' ? 'text-caption' : 'text-body-2'">{{ initials }}</span>
  </v-avatar>
</template>
