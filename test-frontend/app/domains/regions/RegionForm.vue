<script setup lang="ts">
import type { IRegion } from './index'

const props = defineProps<{ modelValue: IRegion; action?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: IRegion] }>()
const { t } = useI18n()

const region = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})
</script>

<template>
  <v-text-field
    v-model="region.nom"
    :label="t('regions.fields.nom')"
    :rules="[(v: string) => !!v || t('regions.fields.nomRequired')]"
  />
  <v-text-field
    v-model.number="region.generation"
    type="number"
    min="1"
    :label="t('regions.fields.generation')"
  />
  <v-textarea
    v-model="region.description"
    :label="t('regions.fields.description')"
  />
</template>
