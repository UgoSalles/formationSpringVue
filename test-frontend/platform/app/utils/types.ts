/**
 * Types partagés du platform. Placés dans `utils/` : Nuxt les auto-importe (valeurs et types) dans
 * la couche comme dans le projet consommateur — aucun import explicite nécessaire.
 */

export interface IAuthUser {
  id: string
  login: string
  email: string
  role: string
}

export type NotificationLevel = 'info' | 'success' | 'warning' | 'error'

export interface INotification {
  id: string
  title: string
  message?: string
  level: NotificationLevel
  read: boolean
  createdAt: Date
}

export interface UserInfo {
  name: string
  role: string
  initials: string
}

export interface NavItem {
  label: string
  /** Cible de navigation. Optionnelle si l'item est un groupe (porte des `children`). */
  to?: string
  /** Nom d'icône MDI optionnel (ex. `'mdi-home'`), rendu par Vuetify. */
  icon?: string
  /** Sous-menu : items enfants (un seul niveau d'imbrication supporté par la sidebar). */
  children?: NavItem[]
}

export interface LegalLink {
  label: string
  to: string
}

/** Métadonnées de pagination renvoyées par le platform sur `POST /ressource/search`. */
export interface IPaginationMeta {
  page: number
  limit: number
  total: number
  totalPages: number
}

/** Enveloppe de réponse paginée standard du platform (`{ data, pagination }`). */
export interface ISearchResponse<T> {
  data: T[]
  pagination: IPaginationMeta
}
