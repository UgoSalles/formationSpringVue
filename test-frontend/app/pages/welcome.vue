<script setup lang="ts">
definePageMeta({ public: true, layout: false })

const { t } = useI18n()
const { login, isAuthenticated } = useAuth()

if (isAuthenticated.value) {
  await navigateTo('/')
}
</script>

<template>
  <v-app>
    <v-main>
      <v-container class="fill-height d-flex flex-column align-center justify-center ga-4">
        <p class="platform-muted">{{ t('public.tagline') }}</p>
        <v-btn color="primary" @click="login">{{ t('auth.loginButton') }}</v-btn>
      </v-container>
    </v-main>
  </v-app>
</template>
