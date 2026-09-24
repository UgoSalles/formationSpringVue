import PokemonForm from './PokemonForm.vue'

export interface IPokemon {
  id: string
  nom: string
  types: string[]
  description: string
  regionId: string
  nature: string
  talents: string[]
  poids: number
  taille: number
  pv: number
  attaque: number
  defense: number
  attaqueSpeciale: number
  defenseSpeciale: number
  vitesse: number
}

// Export NOMMÉ <Pascal(dossier)>Domain — obligatoire (l'export default n'est pas reconnu).
export const PokemonsDomain = definePlatformDomain<IPokemon>({
  name: 'pokemons',
  endpoint: 'pokemons', // sans slash : clé du domaine + segment d'URL
  defaultValue: {
    nom: '',
    types: [],
    description: '',
    regionId: '',
    nature: 'DOCILE',
    talents: [],
    poids: 1,
    taille: 1,
    pv: 1,
    attaque: 1,
    defense: 1,
    attaqueSpeciale: 1,
    defenseSpeciale: 1,
    vitesse: 1,
  },
  formComponent: PokemonForm, // obligatoire pour create/edit
  searchField: 'nom', // la barre de recherche filtre ce champ en LIKE
  tableHeaders: [
    { title: 'pokemons.headers.nom', key: 'nom' },
    { title: 'pokemons.headers.types', key: 'types' },
  ],
  titles: {
    list: 'pokemons.titles.list',
    create: 'pokemons.titles.create',
    edit: 'pokemons.titles.edit',
    view: 'pokemons.titles.view',
  },
})
