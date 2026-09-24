/**
 * Charge l'identité courante au démarrage de l'application (équivalent du `useEffect` de montage de
 * l'ancien `AuthProvider`). Client uniquement (SPA). S'exécute après le plugin `01.api` (`$api`
 * disponible). Le middleware d'auth global attend la fin du chargement avant de décider.
 */
export default defineNuxtPlugin(async () => {
  await useAuth().reload()
})
