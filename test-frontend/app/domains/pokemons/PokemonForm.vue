<script setup lang="ts">
import type { IPokemon } from './index'
import { regionService, type IRegionOption } from '~/services/region-service'

const props = defineProps<{ modelValue: IPokemon; action?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: IPokemon] }>()
const { t } = useI18n()

const pokemon = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

// Catalogues fixes de l'entité backend (enums Java PokemonType / Nature) — recopiés tels quels,
// pas d'endpoint dédié pour les lister.
const typeOptions = [
  'NORMAL', 'FEU', 'EAU', 'PLANTE', 'ELECTRIK', 'GLACE', 'COMBAT', 'POISON',
  'SOL', 'VOL', 'PSY', 'INSECTE', 'ROCHE', 'SPECTRE', 'DRAGON', 'TENEBRES', 'ACIER', 'FEE',
]
const natureOptions = [
  'ASSURE', 'BIZARRE', 'BRAVE', 'CALME', 'DISCRET', 'DOCILE', 'DOUX', 'FOUFOU',
  'GENTIL', 'HARDI', 'JOVIAL', 'LACHE', 'MALIN', 'MALPOLI', 'MAUVAIS', 'MODESTE',
  'NAIF', 'PRESSE', 'PRUDENT', 'PUDIQUE', 'RELAX', 'RIGIDE', 'SERIEUX', 'SOLO', 'TIMIDE',
]

const regionOptions = ref<IRegionOption[]>([])
onMounted(async () => {
  const res = await regionService.listAll()
  regionOptions.value = res.data
})
</script>

<template>
  <v-text-field
    v-model="pokemon.nom"
    :label="t('pokemons.fields.nom')"
    :rules="[(v: string) => !!v || t('pokemons.fields.nomRequired')]"
  />

  <v-select
    v-model="pokemon.regionId"
    :items="regionOptions"
    item-title="nom"
    item-value="id"
    :label="t('pokemons.fields.region')"
  />

  <v-select
    v-model="pokemon.types"
    :items="typeOptions"
    multiple
    chips
    :label="t('pokemons.fields.types')"
  />

  <v-select
    v-model="pokemon.nature"
    :items="natureOptions"
    :label="t('pokemons.fields.nature')"
  />

  <v-combobox
    v-model="pokemon.talents"
    multiple
    chips
    :label="t('pokemons.fields.talents')"
  />

  <v-textarea
    v-model="pokemon.description"
    :label="t('pokemons.fields.description')"
  />

  <v-text-field v-model.number="pokemon.poids" type="number" min="0" :label="t('pokemons.fields.poids')" />
  <v-text-field v-model.number="pokemon.taille" type="number" min="0" :label="t('pokemons.fields.taille')" />
  <v-text-field v-model.number="pokemon.pv" type="number" min="0" :label="t('pokemons.fields.pv')" />
  <v-text-field v-model.number="pokemon.attaque" type="number" min="0" :label="t('pokemons.fields.attaque')" />
  <v-text-field v-model.number="pokemon.defense" type="number" min="0" :label="t('pokemons.fields.defense')" />
  <v-text-field v-model.number="pokemon.attaqueSpeciale" type="number" min="0" :label="t('pokemons.fields.attaqueSpeciale')" />
  <v-text-field v-model.number="pokemon.defenseSpeciale" type="number" min="0" :label="t('pokemons.fields.defenseSpeciale')" />
  <v-text-field v-model.number="pokemon.vitesse" type="number" min="0" :label="t('pokemons.fields.vitesse')" />
</template>
