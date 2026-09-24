/**
 * Adaptateur HTTP du facilitateur CRUD (`$crudApi`) — branché sur `@innobdx/nuxt-crud` via
 * `crud.customFetchKey: '$crudApi'`. C'est l'« adaptateur écrit une fois dans la couche » qui réconcilie
 * le repository HTTP du module avec le contrat platform, **sans fork**.
 *
 * Le module liste en **pagination serveur native** : son repository appelle
 * `GET endpoint?page={0-based}&size={n}&sort={champ,desc}` (et, quand une recherche est active,
 * `GET endpoint/search?{champ}={terme}&page=…`). Le platform, lui, expose la liste via
 * `POST endpoint/search` avec `page` (1-based), `limit` et `sort` (`champ:asc`/`champ:desc`) en query params,
 * et les **filtres au corps** (§3.6). Ce client intercepte donc le GET de liste (et de recherche) et le
 * réécrit en `POST endpoint/search` ; les autres opérations (findById/create/update/remove) collent déjà
 * au platform et passent telles quelles.
 *
 * Pourquoi pas `customCrud.readAll` ? Parce que le module bascule alors en pagination *client-side*
 * (cf. `CrudReadAllView.isServerSide`), ce qui supprime le re-fetch sur changement de page/limite/tri.
 *
 * Tout transite par `$api` (cookies HttpOnly, refresh 401) : l'auth reste agnostique de l'IdP.
 */
export default defineNuxtPlugin(() => {
  const { $api } = useNuxtApp()

  /** Query params de pagination/tri du module — tout le reste est interprété comme un filtre. */
  const PAGINATION_PARAMS = new Set(['page', 'size', 'sort'])

  /**
   * Réécrit le tri du format Spring du module (`champ,desc` / `champ`) vers le format platform
   * (`champ:desc` / `champ:asc`, §3.6). Le module ne trie que sur **un** champ (`CrudReadAllView` ne
   * garde que `sortBy[0]`) ; le back accepte le multi-champ, mais le facilitateur n'en produit qu'un.
   */
  const toPlatformSort = (springSort: string): string => {
    const [field = '', direction] = springSort.split(',')
    return `${field}:${direction === 'desc' ? 'desc' : 'asc'}`
  }

  const crudApi = <T>(request: string, opts: Record<string, unknown> = {}): Promise<T> => {
    const method = String(opts.method ?? 'GET').toUpperCase()
    const [path, qs] = request.split('?')
    const segments = path.split('/')
    const last = segments[segments.length - 1]

    // Liste : GET sur l'endpoint nu. Recherche : GET sur `endpoint/search` (searchEndpoint='search',
    // cf. definePlatformDomain `searchField`). Tout autre GET avec segment (findById, sous-ressource)
    // et tous les autres verbes passent tels quels.
    const isSearch = method === 'GET' && last === 'search'
    const isList = method === 'GET' && !path.includes('/')
    if (!isList && !isSearch) {
      return $api<T>(request, opts)
    }

    const endpoint = isSearch ? path.slice(0, -'/search'.length) : path
    const params = new URLSearchParams(qs ?? '')

    const query: Record<string, string | number> = {}
    const page = params.get('page')
    if (page !== null) {
      query.page = Number(page) + 1 // module 0-based → platform 1-based
    }
    const size = params.get('size')
    if (size !== null) {
      query.limit = Number(size)
    }
    // Le module initialise `pagination.sort = idField` → il transmet `sort=id` (asc) par DÉFAUT, ce
    // qui écraserait le `@DefaultSort` de l'entité côté back. On l'omet : sans tri explicite, c'est au
    // back de décider (`@DefaultSort`, sinon `id ASC`), et `id` est de toute façon ajouté en tie-breaker
    // (cf. §3.3). Tout tri réel de l'utilisateur (y compris `id` desc → `id,desc`) est bien transmis.
    const sort = params.get('sort')
    if (sort && sort !== 'id') {
      query.sort = toPlatformSort(sort)
    }

    // Filtres : tout query param hors pagination/tri (ex. le `searchParam` du domaine) devient un
    // filtre LIKE au corps du POST /search (§3.6). Insensible casse/accents, recherche « contient ».
    const filters: Record<string, unknown> = {}
    for (const [key, value] of params.entries()) {
      if (PAGINATION_PARAMS.has(key) || value.trim() === '') {
        continue
      }
      filters[key] = {
        operator: 'LIKE',
        value,
        options: { caseSensitive: false, accentSensitive: false, anyBefore: true, anyAfter: true },
      }
    }

    return $api<T>(`${endpoint}/search`, { method: 'POST', query, body: { filters } })
  }

  return { provide: { crudApi } }
})
