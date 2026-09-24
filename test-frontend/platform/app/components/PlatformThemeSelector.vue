<script setup lang="ts">
// Bascule de thème via l'API Vuetify (`useTheme`). Le thème vit sur le `<v-app>` ; la persistance
// (préférence utilisateur) relève d'MDC (cf. §3.12), pas du front.
const theme = useTheme()
const { t } = useI18n()
const { platform } = useAppConfig()

// Liste depuis la source unique (theme.config → app.config). `label` = clé i18n (résolue, sinon
// littéral). Aucun nom de thème codé en dur ici.
const themes = computed(() => platform.themes.map((opt) => ({ value: opt.value, label: t(opt.label) })))

const current = computed({
  get: () => theme.global.name.value,
  set: (value: string) => {
    theme.global.name.value = value
  },
})
</script>

<template>
  <v-select
    v-model="current"
    :items="themes"
    item-title="label"
    item-value="value"
    density="compact"
    variant="solo-filled"
    flat
    rounded="lg"
    hide-details
    :aria-label="$t('theme.label')"
    class="platform-control-md"
  />
</template>
