<script setup lang="ts">
const { locale, setLocale } = useI18n()

const LANGUAGES = [
  { value: 'fr', label: 'FR' },
  { value: 'en', label: 'EN' },
]
</script>

<template>
  <v-select
    :model-value="locale"
    :items="LANGUAGES"
    item-title="label"
    item-value="value"
    density="compact"
    variant="solo-filled"
    flat
    rounded="lg"
    hide-details
    aria-label="Langue"
    class="platform-control-sm"
    @update:model-value="setLocale($event as 'fr' | 'en')"
  />
</template>
