<script setup lang="ts">
import type { NavItem } from '../utils/types'

const props = defineProps<{ appName: string; navItems: NavItem[] }>()
const emit = defineEmits<{ close: [] }>()
const { mobile } = useDisplay()
const route = useRoute()

function onNavigate(): void {
  if (mobile.value) emit('close')
}

// Navigation « drill-down » : la nav glisse pour n'afficher qu'un dossier à la fois, avec retour.
const activeGroup = ref<NavItem | null>(
  props.navItems.find((it) => it.children?.some((c) => c.to === route.path)) ?? null,
)
const drilled = ref(activeGroup.value !== null)

function drillTo(item: NavItem): void {
  activeGroup.value = item
  drilled.value = true
}
function back(): void {
  drilled.value = false
}
</script>

<template>
  <div class="platform-sidebar">
    <!-- Partie 1 : le menu (panneau dédié, padding réduit) -->
    <div class="platform-sidebar__menu platform-panel">
      <div class="platform-brand">
        <span class="platform-brand__mark">{{ appName.charAt(0).toUpperCase() }}</span>
        <span class="platform-brand__text">
          <span class="platform-brand__name text-truncate">{{ appName }}</span>
        </span>
        <v-btn
          v-if="mobile"
          icon="mdi-close"
          variant="text"
          size="small"
          class="ml-auto"
          :aria-label="$t('actions.close')"
          @click="emit('close')"
        />
      </div>

      <nav class="platform-nav">
        <div class="platform-nav__viewport">
          <div class="platform-nav__track" :class="{ 'platform-nav__track--sub': drilled }">
            <!-- Panneau racine -->
            <div class="platform-nav__panel">
              <span class="platform-nav__section">Navigation</span>
              <template v-for="item in navItems" :key="item.to ?? item.label">
                <button
                  v-if="item.children?.length"
                  type="button"
                  class="platform-nav__item platform-nav__group"
                  @click="drillTo(item)"
                >
                  <v-icon v-if="item.icon" :icon="item.icon" size="20" />
                  <span class="platform-nav__label">{{ $t(item.label) }}</span>
                  <v-icon class="platform-nav__chev" icon="mdi-chevron-right" size="18" />
                </button>
                <NuxtLink
                  v-else
                  :to="item.to"
                  class="platform-nav__item"
                  exact-active-class="platform-nav__item--active"
                  @click="onNavigate"
                >
                  <v-icon v-if="item.icon" :icon="item.icon" size="20" />
                  <span class="platform-nav__label">{{ $t(item.label) }}</span>
                </NuxtLink>
              </template>
            </div>

            <!-- Panneau dossier (sous-menu) -->
            <div class="platform-nav__panel">
              <button type="button" class="platform-nav__item platform-nav__back" @click="back">
                <v-icon icon="mdi-chevron-left" size="20" />
                <span class="platform-nav__label">{{ activeGroup ? $t(activeGroup.label) : '' }}</span>
              </button>
              <NuxtLink
                v-for="child in activeGroup?.children ?? []"
                :key="child.to ?? child.label"
                :to="child.to"
                class="platform-nav__item platform-nav__item--child"
                exact-active-class="platform-nav__item--active"
                @click="onNavigate"
              >
                <span class="platform-nav__bullet" />
                <span class="platform-nav__label">{{ $t(child.label) }}</span>
              </NuxtLink>
            </div>
          </div>
        </div>
      </nav>
    </div>

    <!-- Partie 2 : encart séparé. Zone PERSONNALISABLE par le projet via le slot `extra` (graphe métier,
         raccourcis, résumé… au choix). Repli par défaut : un encart pointillé qui signale la zone.
         L'identité/déconnexion vit désormais dans le menu de l'avatar (header) → cette zone remplit
         tout l'espace restant. Pour customiser : remplir le slot `#extra` (ou, en pro, éditer ce composant inliné). -->
    <div class="platform-sidebar__aux platform-panel">
      <slot name="extra">
        <div class="platform-placeholder">Zone personnalisable</div>
      </slot>
    </div>
  </div>
</template>
