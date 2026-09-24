<template>
  <NuxtLayout>
    <NuxtPage>
      <template #default="{ Component }">
        <div class="page-root">
          <component :is="Component" />
        </div>
      </template>
    </NuxtPage>
  </NuxtLayout>
</template>
