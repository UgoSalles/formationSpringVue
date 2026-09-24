import RegionForm from './RegionForm.vue'

export interface IRegion {
  id: string
  nom: string
  generation: number
  description?: string
}

// Export NOMMÉ <Pascal(dossier)>Domain — obligatoire (l'export default n'est pas reconnu).
export const RegionsDomain = definePlatformDomain<IRegion>({
  name: 'regions',
  endpoint: 'regions', // sans slash : clé du domaine + segment d'URL
  defaultValue: { nom: '', generation: 1, description: '' },
  formComponent: RegionForm, // obligatoire pour create/edit
  searchField: 'nom', // la barre de recherche filtre ce champ en LIKE
  tableHeaders: [
    { title: 'regions.headers.nom', key: 'nom' },
    { title: 'regions.headers.generation', key: 'generation' },
  ],
  titles: {
    list: 'regions.titles.list',
    create: 'regions.titles.create',
    edit: 'regions.titles.edit',
    view: 'regions.titles.view',
  },
})
