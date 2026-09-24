<script setup lang="ts">
import type { LegalLink } from '../utils/types'

withDefaults(defineProps<{ version?: string; legalLinks?: LegalLink[] }>(), {
  version: '',
  legalLinks: () => [],
})
</script>

<template>
  <footer class="platform-footer">
    <nav class="platform-footer__links">
      <NuxtLink
        v-for="link in legalLinks"
        :key="link.to"
        :to="link.to"
        class="platform-footer-link"
      >
        {{ $t(link.label) }}
      </NuxtLink>
    </nav>
    <span v-if="version" class="platform-version font-monospace">{{ version }}</span>
  </footer>
</template>
