import { AbstractService } from '#imports'
import type { ISearchResponse } from '#imports'

export interface IRegionOption {
  id: string
  nom: string
}

class RegionService extends AbstractService {
  protected basePath = '/regions'

  // Liste complète des régions (catalogue fixe, peu volumineux) — utilisé pour peupler
  // le sélecteur de région du formulaire Pokémon.
  public listAll() {
    return this.search<ISearchResponse<IRegionOption>>({ pagination: { page: 1, limit: 100 } })
  }
}

export const regionService = new RegionService()
