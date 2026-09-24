<script setup lang="ts">
definePageMeta({ public: true })

const { t } = useI18n()
</script>

<template>
  <article class="max-w-3xl mx-auto py-8">
    <h1 class="page-title mb-4">{{ t('legal.privacy') }}</h1>
    <p class="text-muted">{{ t('legal.placeholder') }}</p>
  </article>
</template>
