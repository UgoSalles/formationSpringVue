import type { CrudDomain, CrudDomainInput } from '@innobdx/nuxt-crud'

/**
 * Adaptateur de réponse platform pour `@innobdx/nuxt-crud` (option `responseFormat`).
 *
 * La liste passe par le **mode serveur natif** du module : le repository HTTP appelle
 * `GET endpoint?page&size&sort` que le client `$crudApi` (cf. plugin `04.crud-api`) réécrit en
 * `POST endpoint/search` (convention platform, §3.6). La réponse est l'enveloppe platform
 * `{ data, pagination: { total, … } }` ; cet adaptateur en extrait `{ data, total }` (le seul
 * contrat attendu par le module). Le back garde l'enveloppe complète — exploitable par des
 * consommateurs externes.
 *
 * NB : on ne passe **pas** par `customCrud.readAll` — le module bascule alors en pagination
 * client-side (cf. `CrudReadAllView.isServerSide`), ce qui désactive le re-fetch sur changement de
 * page/limite/tri. Le mode serveur natif + adaptateur préserve la pagination serveur.
 */
const platformResponseFormat = {
  parse: (response: unknown) => {
    const envelope = response as ISearchResponse<Record<string, unknown>> | undefined
    return {
      data: envelope?.data ?? [],
      total: envelope?.pagination?.total ?? 0,
    }
  },
  parseSingle: (response: unknown) => response,
}

/**
 * Résout une chaîne via vue-i18n (instance globale `@nuxtjs/i18n`), de façon réactive au changement
 * de locale. Une chaîne qui n'est PAS une clé i18n connue est renvoyée telle quelle — comportement
 * natif de vue-i18n : `t('Widgets')` → `'Widgets'`. On peut donc écrire indifféremment une clé
 * (`'examples.titles.list'`) ou un libellé littéral (`'Widgets'`) dans la config du domaine.
 *
 * Hors contexte Nuxt (certains tests unitaires), retombe sur la chaîne brute.
 */
function translate(value: string): string {
  try {
    const i18n = useNuxtApp().$i18n as { t?: (key: string) => string } | undefined
    return i18n?.t ? i18n.t(value) : value
  } catch {
    return value
  }
}

/**
 * Réécrit `titles` et `tableHeaders[].title` en **getters** résolus par {@link translate}.
 *
 * Le module rend ces valeurs en chaîne brute (aucun i18n interne) ; en les exposant via des getters,
 * l'accès dans le rendu Vuetify dépend de la locale → les titres se traduisent (et restent réactifs
 * pour ceux lus en `computed`, cf. `CrudActionView`). `defineCrudDomain` ne fait qu'un spread shallow :
 * les références `titles`/`tableHeaders` (porteuses des getters) sont préservées.
 */
function withI18n<T extends Record<string, unknown> & { id: string }>(
  config: PlatformDomainInput<T>,
): PlatformDomainInput<T> {
  const titles = {} as typeof config.titles
  for (const [key, raw] of Object.entries(config.titles)) {
    Object.defineProperty(titles, key, {
      enumerable: true,
      configurable: true,
      get: () => translate(raw as string),
    })
  }

  const tableHeaders = config.tableHeaders.map((header) => {
    if (typeof header.title !== 'string') {
      return header
    }
    const raw = header.title
    const resolved = { ...header }
    Object.defineProperty(resolved, 'title', {
      enumerable: true,
      configurable: true,
      get: () => translate(raw),
    })
    return resolved
  })

  return { ...config, titles, tableHeaders }
}

/**
 * Configuration d'un domaine platform : identique à `CrudDomainInput` du module, mais `idField`,
 * `updateMethod` et `responseFormat` (enveloppe platform) sont fournis par défaut (non requis ici).
 *
 * Ajout platform : `searchField` — le champ filtré (opérateur `LIKE`) par la barre de recherche du
 * module. Quand il est renseigné, le terme saisi est transmis au back comme filtre `POST /search`
 * (cf. plugin `04.crud-api`). Le champ doit être autorisé côté back via
 * `@Expose(SEARCH, allowedFilters = {@Filter(field = "...", operators = {LIKE})})`.
 */
export type PlatformDomainInput<T extends Record<string, unknown>> = CrudDomainInput<T, 'id'> & {
  /** Champ filtré en `LIKE` par la barre de recherche (active la recherche serveur). */
  searchField?: keyof T & string
}

/**
 * Déclare un domaine CRUD aligné sur le contrat du platform, par-dessus `defineCrudDomain` de
 * `@innobdx/nuxt-crud`. Injecte les conventions platform :
 * - `idField: 'id'` — ULID de `BaseEntity`, partagé par toutes les entités du platform ;
 * - `updateMethod: 'PATCH'` — le platform expose PATCH, pas PUT ;
 * - `responseFormat` → adaptateur d'enveloppe platform (cf. {@link platformResponseFormat}).
 *
 * Pagination **serveur native** du module : `findAll`/`findOne`/`create`/`update`/`remove` passent
 * par le repository HTTP par défaut, branché sur le client `$crudApi` (`customFetchKey`) qui réécrit
 * le `GET endpoint?page&size` de liste en `POST endpoint/search` (cf. plugin `04.crud-api`) ; les
 * autres verbes (GET/POST/PATCH/DELETE `/ressource/:id`) collent déjà au platform.
 *
 * **i18n** : les valeurs de `titles` et de `tableHeaders[].title` sont traitées comme des clés i18n
 * (résolues via vue-i18n, réactives à la locale). Un libellé littéral non-clé est rendu tel quel
 * (cf. {@link translate}) — l'exemple de la doc reste donc valide sans traduction.
 *
 * @typeParam T - interface de la ressource ; doit exposer un `id: string` (l'ULID du platform)
 * @param config - configuration du domaine (endpoint, tableHeaders, titles, defaultValue…)
 * @returns le domaine CRUD, à exposer en **export nommé** `<Pascal(dossier)>Domain` depuis
 *          `app/domains/<x>/index.ts` (convention `@innobdx/nuxt-crud` : il génère
 *          `import { WidgetsDomain } from '~/domains/widgets'` — l'export default n'est pas reconnu)
 *
 * @example
 * ```ts
 * // app/domains/widgets/index.ts
 * export const WidgetsDomain = definePlatformDomain<IWidget>({
 *   name: 'widgets',
 *   endpoint: 'widgets',
 *   defaultValue: { name: '', quantity: 0 },
 *   tableHeaders: [
 *     { title: 'Nom', key: 'name' },
 *     { title: 'Quantité', key: 'quantity' },
 *   ],
 *   titles: { list: 'Widgets', create: 'Nouveau widget', edit: 'Modifier', view: 'Détail' },
 * })
 * ```
 *
 * @remarks Pour un identifiant technique autre que `id`, utiliser directement `defineCrudDomain`.
 */
export function definePlatformDomain<T extends Record<string, unknown> & { id: string }>(
  config: PlatformDomainInput<T>,
): CrudDomain<T, 'id'> {
  const { searchField, ...rest } = config
  const localized = withI18n(rest as PlatformDomainInput<T>)
  // `searchField` (sucre platform) → mécanisme de recherche du module : le terme part en query param
  // `?<champ>=<terme>` vers `endpoint/search`, que `$crudApi` convertit en filtre LIKE du POST /search.
  const search = searchField
    ? { searchEndpoint: 'search', searchParam: searchField }
    : {}
  return defineCrudDomain<T>()('id', {
    updateMethod: 'PATCH',
    ...localized,
    ...search,
    responseFormat:
      localized.responseFormat ??
      (platformResponseFormat as CrudDomainInput<T, 'id'>['responseFormat']),
  })
}
