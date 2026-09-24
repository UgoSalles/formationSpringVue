# Guide du projet — test

Guide unique d'utilisation du projet, généré par le platform technique. Il décrit **comment travailler
dans ce projet** : conventions, briques fournies, et façons de faire recommandées.

> **Philosophie du platform : fermé par défaut, ouvert par déclaration explicite.** Tout ce qui n'est
> pas déclaré (route, opération CRUD, filtre, page publique…) n'existe pas.

## Sommaire

- [Démarrage rapide](#démarrage-rapide)
- **Backend** — [Anatomie d'une feature](#anatomie-dune-feature-crud-référence) · [Données](#données--entités-dtos-migrations) · [API](#api--services-crud-controllers-rest) · [Recherche](#recherche--filtres-querydsl) · [Sécurité & erreurs](#sécurité--erreurs)
- **Frontend** — [Structure](#structure) · [Données & services](#données--services) · [Auth, i18n & thèmes](#auth-i18n--thèmes)
- **Tests** — [Backend](#tests-backend) · [Frontend](#tests-frontend)
- **[Bonnes pratiques (transverses)](#bonnes-pratiques-transverses)** — conventions, sécurité, perf, do/don't

## Démarrage rapide

Le **back et le front se lancent indépendamment** — Docker n'est **pas** requis (ni `docker compose`).

**Backend** — il lui faut une base PostgreSQL accessible (cf. `DB_URL` dans `.env`) :

```bash
cd backend
./mvnw spring-boot:run          # démarre l'API (lit .env / application.yml)
```

**Frontend** — SPA Nuxt qui proxifie `/api` vers le back :

```bash
cd frontend
npm install                     # la première fois
npm run dev                     # serveur de dev
```

**Avec Docker** (optionnel, si l'environnement le fournit) :

```bash
docker compose up -d            # backend + frontend + traefik
docker compose logs -f
```

Voir aussi `ONBOARDING.md` à la racine pour la mise en route complète.

---

# Backend (Spring Boot)

## Anatomie d'une feature CRUD (référence)

Comment ajouter une ressource de bout en bout, **back + front**. C'est une **page de référence** : le
platform ne *scaffolde* aucune feature dans le projet (philosophie « fermé par défaut » — rien n'existe
tant que tu ne l'as pas déclaré). Tu crées les fichiers ci-dessous toi-même.

On prend `Entite` (champs `name`, `quantity`) comme exemple. La tranche verticale :

```
Entité  →  DtoGenerator  →  Repository  →  Service  →  Controller        (back)
                                          Domaine  →  Formulaire        (front)
```

### Backend, étape par étape

**1. L'entité** — hérite de `BaseEntity` (id `UUID` uuid_v7 + audit, cf. [Données](#données--entités-dtos-migrations)).
Les contraintes Bean Validation portées ici sont **recopiées dans les DTOs** par le générateur. Note :
**pas de `groups` sur l'entité** — le générateur ajoute lui-même `groups = Create/Update` aux DTOs.

```java
@Entity
@DefaultSort(field = "createdAt", order = SortOrder.DESC) // tri par défaut quand search ne précise rien
public class Entite extends BaseEntity {

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @PositiveOrZero
    @Column(nullable = false)
    private int quantity;
}
```

**2. Les DTOs + le mapper** — `java tools/DtoGenerator.java Entite` (depuis `backend/`) génère
`EntiteCreateDto`, `EntiteUpdateDto` (= create + `id`), `EntiteDetailDto` (tous les champs) et
`EntiteMapper` (MapStruct), avec les contraintes de l'entité recopiées et scopées (`groups`). Ce sont
ensuite **des fichiers normaux** : modifiables, commités, jamais régénérés. (`EntiteSummaryDto` n'est pas
généré — à créer si besoin d'une liste allégée.)

**3. Le repository**

```java
public interface EntiteRepository extends JpaRepository<Entite, UUID> { }
```

**4. Le service** — hérite de `BaseCrudService<Entity, CreateDto, UpdateDto, ResponseDto>` (4 paramètres).
Fournit `search` / `findOne` / `create` / `update` / `remove`, le cache et les transactions
(cf. [API](#api--services-crud-controllers-rest)).

```java
@Service
public class EntiteService
    extends BaseCrudService<Entite, EntiteCreateDto, EntiteUpdateDto, EntiteDetailDto> {

    private final EntiteRepository repository;
    private final JPAQueryFactory queryFactory;

    public EntiteService(EntiteRepository repository, JPAQueryFactory queryFactory) {
        this.repository = repository;
        this.queryFactory = queryFactory;
    }

    @Override protected JpaRepository<Entite, UUID> getRepository() { return repository; }
    @Override protected JPAQueryFactory getQueryFactory()          { return queryFactory; }
    @Override protected EntityPath<Entite> getEntityPath()         { return QEntite.entite; }
    @Override protected Class<Entite> getEntityClass()             { return Entite.class; }
    @Override public String getCacheName()                         { return "entites"; }

    // Mapping (déléguer au EntiteMapper généré, ou inline) :
    @Override protected Entite toEntity(EntiteCreateDto dto) { /* ... */ }
    @Override protected void merge(Entite e, EntiteUpdateDto dto) { /* ... */ }
    @Override protected EntiteDetailDto toDto(Entite e) { /* ... */ }
}
```

**5. Le controller** — exposition explicite. Rien d'implicite : une opération **non déclarée** renvoie
404. Les filtres `search` autorisés se déclarent **dans le `@Expose(SEARCH)`** (cf. [Recherche](#recherche--filtres-querydsl)).

```java
@RestController
@RequestMapping("/api/entites")
@Expose(CREATE) @Expose(FIND_ONE)                   // UPDATE / REMOVE non déclarés = inexistants
@Expose(value = SEARCH, allowedFilters = {          // filtres autorisés portés par le SEARCH lui-même
    @Filter(field = "name",     operators = {LIKE, EQ}),
    @Filter(field = "quantity", operators = {EQ, GT, LT}),
})
public class EntiteController
    extends AbstractCrudController<Entite, EntiteCreateDto, EntiteUpdateDto, EntiteDetailDto> {

    private final EntiteService service;
    public EntiteController(EntiteService service) { this.service = service; }

    @Override
    protected BaseCrudService<Entite, EntiteCreateDto, EntiteUpdateDto, EntiteDetailDto> getService() {
        return service;
    }
}
```

Endpoints obtenus : `POST /api/entites` · `GET /api/entites/{id}` · `POST /api/entites/search`.

**6. Le schéma** — rien à écrire : en dev Hibernate suit les entités (`ddl-auto: update`), en prod la
migration Flyway se génère depuis les entités JPA (cf. [Données — schéma dev vs prod](#schéma-de-base--dev-vs-prod)).

### Frontend, étape par étape

Le CRUD front s'appuie sur `@innobdx/nuxt-crud` via le helper `definePlatformDomain` (couche
`@platform/front`, auto-importé). Un **dossier = un domaine** : `app/domains/<x>/index.ts` (scanné au build).

**1. Le domaine**

```ts
// app/domains/entites/index.ts
import EntiteForm from './EntiteForm.vue'

export interface IEntite {
  id: string          // un UUID se sérialise en string JSON
  name: string
  quantity: number
}

// Export NOMMÉ <Pascal(dossier)>Domain (ici EntitesDomain) — l'export default n'est pas reconnu.
export const EntitesDomain = definePlatformDomain<IEntite>({
  name: 'entites',
  endpoint: 'entites',          // sans slash : clé $crudDomains + segment d'URL
  defaultValue: { name: '', quantity: 0 },
  formComponent: EntiteForm,    // obligatoire pour create/edit
  searchField: 'name',          // la barre de recherche filtre ce champ en LIKE (cf. ci-dessous)
  tableHeaders: [
    { title: 'entites.headers.name', key: 'name' },        // title = clé i18n
    { title: 'entites.headers.quantity', key: 'quantity' },
  ],
  titles: { list: 'entites.titles.list', create: 'entites.titles.create',
            edit: 'entites.titles.edit', view: 'entites.titles.view' },
})
```

`definePlatformDomain` injecte les conventions platform : `idField: 'id'`, `updateMethod: 'PATCH'`, et
`readAll → POST /entites/search` (exige donc `@Expose(SEARCH)` côté controller). Les autres opérations
retombent sur les verbes HTTP standard.

**Recherche & tri (état actuel).**
- **Tri** : clic sur l'entête de colonne (data-table serveur) → re-fetch trié. **Un seul champ à la fois**
  (le module ne conserve qu'un critère de tri). Le back gère le multi-champ, mais le module ne l'émet pas.
- **Recherche** : avec `searchField`, la barre de recherche du module filtre **ce champ** en `LIKE`
  (terme → `POST /search` avec `{ filters: { name: { operator: 'LIKE', … } } }`). Le champ doit être
  **autorisé côté back** : `@Expose(SEARCH, allowedFilters = {@Filter(field = "name", operators = {LIKE})})`.
- **Limites (backlog)** : recherche multi-colonnes / cumul de filtres / champ « global » (OR sur plusieurs
  colonnes) / tri multi-champ ne sont **pas** fournis par le module sans le forker — à traiter via une vue
  liste custom (composable `useCrud`) + un filtre `OR` côté back, plus tard.

**2. Le formulaire** — `v-model` = l'item courant, ré-émis via `update:modelValue` ; monté par le module
dans son `<VForm>`.

```vue
<script setup lang="ts">
import type { IEntite } from './index'
const props = defineProps<{ modelValue: IEntite; action?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: IEntite] }>()
const { t } = useI18n()
const entite = computed({ get: () => props.modelValue, set: (v) => emit('update:modelValue', v) })
</script>

<template>
  <v-text-field v-model="entite.name" :label="t('entites.fields.name')"
                :rules="[(v: string) => !!v || t('entites.fields.nameRequired')]" />
  <v-text-field v-model.number="entite.quantity" type="number" min="0"
                :label="t('entites.fields.quantity')" />
</template>
```

**3. i18n + entrée de menu** — ajoute `i18n/locales/{fr,en}/entites.json` (les clés `entites.*`
référencées) et l'entrée de navigation dans `app.config.ts` :

```ts
navItems: [
  { label: 'nav.home', to: '/', icon: 'mdi-home' },
  { label: 'nav.entites', to: '/entites', icon: 'mdi-cube-outline' },
],
```

> **À retenir** : tout est explicite. Tant qu'une entité, une opération CRUD, un filtre ou une page
> publique n'est pas **déclaré**, il n'existe pas.

---

## Données — entités, DTOs, migrations

### Entités — `BaseEntity`

Toute entité hérite de `BaseEntity` (`@MappedSuperclass`), qui fournit l'identité et l'audit :

| Champ | Type | Détail |
|---|---|---|
| `id` | `UUID` | **uuid_v7** — triable par insertion, type natif (colonne Postgres `uuid`), généré côté application |
| `createdAt` / `updatedAt` | `Instant` | `@CreatedDate` / `@LastModifiedDate` |
| `createdBy` / `updatedBy` | `String` | `sub` du token (utilisateur courant ; fallback `anonymous`) |

- **Convention colonnes `String`** : `nullable = false, length = 100` par défaut. Tout assouplissement
  se déclare explicitement (`@Column(nullable = true, length = 500)`).
- **`@DefaultSort`** : tri appliqué quand le `SearchRequest` n'impose rien. `BaseEntity` en porte un par
  défaut — **`id ASC`** (uuid_v7 → chronologique, ordre stable, pas de bug de pagination). Tu peux le
  **surcharger** sur ton entité (`@DefaultSort(field = "createdAt", order = SortOrder.DESC)`) : il **prime**
  alors sur celui de `BaseEntity`. Le départage final par `id` reste ajouté automatiquement.
- Les contraintes **Bean Validation** (`@NotBlank`, `@Size`, `@Email`…) se déclarent **sur l'entité, sans
  `groups`** : c'est le générateur qui les recopie dans les DTOs en y ajoutant `groups = Create/Update`
  (ce sont les DTOs, pas l'entité, qui pilotent la validation à la requête).

### DTOs — générateur `DtoGenerator`

On ne crée pas les DTOs à la main. Le projet embarque un générateur autonome (JDK seul, **aucun accès au
CLI `platform` requis**) ; depuis `backend/`, `java tools/DtoGenerator.java Entite` génère, à partir de
l'entité `Entite` :

| Généré | Contenu |
|---|---|
| `EntiteCreateDto` | tous les champs **sauf** ceux de `BaseEntity` |
| `EntiteUpdateDto` | `CreateDto` + `id` |
| `EntiteDetailDto` | tous les champs (lazy inclus) |
| `EntiteMapper` | `create/updateDto → entity`, `entity → detailDto` (MapStruct) |

Les contraintes de validation de l'entité sont **lues dans la source et recopiées dans les DTOs au moment
de la génération** (analyse de la source par l'outil — pas de processeur d'annotations compile-time), avec
les `groups` ajoutés. Après génération, **ce sont des fichiers normaux** : modifiables, commités, jamais
régénérés automatiquement. `EntiteSummaryDto` n'est **pas** généré — crée-le si tu as besoin d'une vue allégée.

- **Obligatoire vs optionnel** : à la génération, le `CreateDto` reprend les contraintes Bean Validation
  **présentes sur l'entité**. Un champ porteur d'une **contrainte de présence** (`@NotNull` / `@NotBlank` /
  `@NotEmpty`) devient donc **obligatoire** ; les autres sont **optionnels**. Convention : un champ non
  nullable (`@Column(nullable = false)`) porte aussi `@NotNull`/`@NotBlank` sur l'entité — c'est cette
  annotation qui le rend obligatoire dans le DTO (le `nullable = false` seul ne suffit pas). C'est un
  **point de départ** : les DTOs ne sont générés qu'**une fois**, à toi de les ajuster ensuite.
- **Champ ajouté après coup** : le générateur ne revient pas modifier des fichiers existants (il **ignore**
  les fichiers déjà présents). Ajoute le champ **manuellement** dans les DTOs concernés (avec les bons groupes).
- **Mapping complexe** : champ calculé, transformation custom → ajoute une méthode dans `EntiteMapper`
  directement (MapStruct standard, Java pur, aucune limite).
- **Relations aplaties en UUID (pas d'imbrication — REST)** : le générateur ne recopie **jamais** une
  entité liée dans un DTO. `@ManyToOne`/`@OneToOne` `User user` → `UUID userId` ; `@OneToMany`/`@ManyToMany`
  `List<Document> documents` → `List<UUID> documentIds`. Le **détail** des entités liées se récupère via des
  endpoints dédiés (`@Expose(subResource = "...")`) → le front fait plusieurs requêtes. Le **mapper** reçoit
  alors les entités liées en **paramètres** (`toEntity(dto, User user, List<OrderLine> lines)`) : le **service**
  les charge par UUID (`repo.findById(dto.userId())`, `repo.findAllById(dto.lineIds())`) et les passe déjà
  résolues — le mapper reste pur. En lecture, `toDto` extrait les UUID (to-one nativement, to-many via un
  helper `toId(...)` généré).
- **Update = PATCH partiel** : le `merge` porte `@BeanMapping(nullValuePropertyMappingStrategy = IGNORE)` →
  un champ **null** du DTO **n'écrase pas** la valeur existante. On **ne peut donc pas** forcer une valeur à
  `null` via PATCH (un record ne distingue pas « absent » de « null ») : **effacer** = action métier dédiée
  (`@BusinessRule`), cas rare. Utilise des **types boxés** (`Integer`, pas `int`) : un primitif ne pouvant
  être null, `IGNORE` ne le protège pas (toujours appliqué).

### Schéma de base : dev vs prod

Le schéma se gère **différemment selon l'environnement** :

- **En dev** : pas de migration à écrire. Hibernate **génère et met à jour le schéma automatiquement**
  (`ddl-auto: update`) à partir de tes entités JPA. Tu modifies une entité → la table suit au redémarrage.
  Limite : un changement **destructeur** (ex. un champ `null` qui passe `not null` sur une table déjà
  remplie) ne peut pas s'appliquer en place → il faut repartir d'un schéma propre (cf. *seeder*).
- **En prod** : le schéma est versionné par **Flyway**. Scripts dans `src/main/resources/db/migration/`,
  nommés `V{version}__{description}.sql`, **exécutés automatiquement au démarrage** (et un échec **refuse
  le démarrage** : on ne tourne jamais sur un schéma incohérent). **Un script exécuté ne se modifie
  jamais** — on en ajoute un nouveau. Les migrations se **génèrent depuis les entités JPA**, on n'écrit
  pas le SQL à la main.

> Pourquoi automatique ? Pour garantir qu'**un déploiement = un schéma à jour**, sans étape manuelle ni
> dérive entre environnements. Le dev reste rapide (JPA suit les entités) ; la prod reste sûre et traçable
> (Flyway, versionné, fail-fast).

### Seeder — données de dev & fixtures de test

Pour peupler la base de dev avec des **données réalistes** (et générer des fixtures de test), déclare un
**`Seed<T>` par entité** (bean Spring). `one(faker)` construit **un agrégat réaliste** — l'entité et ses
sous-ressources (en cascade JPA). Tout est du Java pur (Datafaker fournit le `Faker`) :

```java
@Component
public class EntiteSeed implements Seed<Entite> {
    @Override public Class<Entite> type() { return Entite.class; }

    @Override public Entite one(Faker faker) {
        Entite e = new Entite();
        e.setName(faker.commerce().productName());            // donnée réaliste
        e.setStatut(faker.options().option("ACTIF", "INACTIF")); // valeur « métier » imposée
        e.setQuantity(faker.number().numberBetween(0, 100));
        // sous-ressource : cardinalité (éventuellement non uniforme) — du Java, donc tout est permis
        int nbLignes = faker.number().numberBetween(1, 5);
        for (int i = 0; i < nbLignes; i++) {
            e.getLignes().add(new Ligne(faker.commerce().productName()));
        }
        return e;
    }
    // int order() { return 0; }  // surcharge si ce seed dépend d'un autre (FK vers un autre agrégat)
}
```

**Lancer le seeding** (jamais automatique — uniquement sur commande). Le « drop » se fait en recréant le
schéma à neuf (`ddl-auto=create`) ; le seeder remplit ensuite :

```bash
cd backend
# basic : 1 instance par entité (structure minimale réaliste)
./mvnw spring-boot:run -Dspring-boot.run.arguments="--platform.seed.run=basic --spring.jpa.hibernate.ddl-auto=create"
# dev / full : volume passé en commande (ex. 50)
./mvnw spring-boot:run -Dspring-boot.run.arguments="--platform.seed.run=dev --platform.seed.count=50 --spring.jpa.hibernate.ddl-auto=create"
```

- **`basic`** = 1 par entité ; **`dev`/`full`** = `--platform.seed.count=N` par entité (`full` = volume de
  validation/staging réaliste). Sans `--platform.seed.run`, **rien ne se seed** (run normal et prod intacts).
- **Réutilisable en test** : `persist(new EntiteSeed().one(new Faker()))` te donne une fixture cohérente
  sans dupliquer la construction.

---

## API — services CRUD, controllers, REST

### `BaseCrudService`

Le service métier hérite de `BaseCrudService<Entity, CreateDto, UpdateDto, ResponseDto>` et fournit les
paramètres concrets (`getRepository()` / `getQueryFactory()` / `getEntityPath()` / `getEntityClass()` /
`getCacheName()` + le mapping).

> Les filtres `search` ne sont **pas** un type générique : ils passent par le corps de `POST /search`
> (`SearchBody`) et se déclarent dans `@Expose(SEARCH, allowedFilters = {...})` (cf. [Recherche](#recherche--filtres-querydsl)).

Opérations fournies (protégées) : `search` · `findOne` · `create` · `update` · `remove`.

- **`search`** est la liste par défaut : **paginée, triée, filtrée** (`SearchRequest → SearchResponse`).
  C'est ce qu'il te faut dans la quasi-totalité des cas. Pour une ressource **à volumétrie petite et
  bornée** (ex. une table de paramètres de quelques lignes) où pagination/filtres n'apportent rien, tu
  peux exposer à la place une liste simple : ajoute un `findAll()` (renvoyant toute la collection) dans le
  service et l'endpoint correspondant dans le controller, sans déclarer `@Expose(SEARCH)`.
- **Cache** : `@Cacheable` sur `findOne`/`search`, `@CacheEvict` automatique sur `create`/`update`/`remove`.
- **Transactions** : écriture `@Transactional`, lecture `@Transactional(readOnly = true)`.

### `AbstractCrudController` — exposition explicite

Un controller **n'expose que ce qu'il déclare**. Rien d'implicite.

```java
@RestController
@RequestMapping("/api/entites")
@Expose(CREATE) @Expose(FIND_ONE) @Expose(SEARCH)   // UPDATE / REMOVE non déclarés = inexistants (404)
@Expose(subResource = "lines")                       // → GET /api/entites/{id}/lines (relation JPA "lines")
public class EntiteController
    extends AbstractCrudController<Entite, EntiteCreateDto, EntiteUpdateDto, EntiteDetailDto> {

    private final EntiteService service;
    public EntiteController(EntiteService service) { this.service = service; }

    // getService() fournit le service concret typé au controller générique, qui ne peut pas l'autowire
    // lui-même (le type est un paramètre générique, effacé à la compilation) — c'est le point d'injection.
    @Override
    protected BaseCrudService<Entite, EntiteCreateDto, EntiteUpdateDto, EntiteDetailDto> getService() {
        return service;
    }

    @ExposeOverride(CREATE) // surcharge le comportement par défaut
    public ResponseEntity<EntiteDetailDto> create(@RequestBody @Validated(Create.class) EntiteCreateDto dto) {
        return super.create(dto); // + logique custom
    }

    @PostMapping("/{id}/archiver")
    @BusinessRule(code = "RG-ENTITE-001", description = "Archivage avec notification")
    public ResponseEntity<Void> archiver(@PathVariable UUID id) { /* ... */ }
}
```

Trois niveaux :
1. **`@Expose(OP)`** (annotation de classe, répétable) — active une opération CRUD.
2. **`@ExposeOverride(OP)`** (annotation de méthode) — surcharge le comportement.
3. **Méthode libre + `@BusinessRule`** — endpoint métier custom (code `RG-RESSOURCE-NUM` obligatoire).

> **`getService()`** n'est pas du boilerplate inutile : c'est le **point d'injection** du service. Le
> controller abstrait est générique (`<Entity, …>`) et ne peut pas `@Autowired` un type effacé ; la classe
> concrète lui fournit donc son service typé via cette méthode.

> Les `@BusinessRule` sont collectées **au démarrage** (scan par réflexion) dans `business-rules.json`,
> **groupé par entité** et trié par code — un registre versionnable de tes règles métier. Génération
> best-effort (n'échoue jamais le boot), débrayable via `platform.business-rules.enabled=false`.

Autres annotations : `@AllowedFilters` (filtres autorisés sur un **endpoint custom** — méthode ; pour le
`search` standard, c'est `@Expose(SEARCH, allowedFilters = {...})`) · `@AllowedQueryParams` (query params
GET d'exception, ex. `?format=csv` ; non déclaré → 400).

**Sous-ressources** : `@Expose(subResource = "documents")` sur la classe → `GET /api/entites/{id}/documents`.
Le nom du segment = nom de la relation JPA (`@OneToMany` / `@ManyToOne`) sur l'entité ; le routage est
résolu **au runtime par réflexion** (lecture du getter de la relation, dans une transaction de lecture).
Un segment non déclaré renvoie 404.

### Conventions REST (vérifiées par Checkstyle)

| ✅ Autorisé | Sens |
|---|---|
| `GET/HEAD/POST/PATCH/DELETE /ressource` | CRUD standard |
| `POST /ressource/search` | **toutes** les recherches/listes |
| `GET /ressource/{id}` | findOne (uuid_v7) |
| `GET /ressource/by-{champ}/{val}` | uniquement sur `@Column(unique = true)` |
| `GET /ressource/{id}/sous-res` | sous-ressource (`@Expose(subResource = "...")`) |
| `POST /ressource/{id}/action` | action métier (`@BusinessRule` obligatoire, max 3 niveaux) |

❌ Interdits : un **verbe** dans l'URI (`get`, `create`, `rechercher`, `supprimer`…), `by-` sans champ
unique, un GET qui filtre via query params, une route qui duplique le CRUD.

---

## Recherche & filtres (QueryDSL)

Toute liste passe par `POST /ressource/search`. Le filtrage repose sur une **triple protection** : liste
blanche de filtres → opérateurs typés → requêtes paramétrées QueryDSL.

### Déclarer les filtres autorisés

Les filtres autorisés du `search` se déclarent **dans le `@Expose(SEARCH)`**, via `allowedFilters` :

```java
@Expose(value = SEARCH, allowedFilters = {
    @Filter(field = "name",      operators = {LIKE}),
    @Filter(field = "status",    operators = {EQ, IN}),
    @Filter(field = "createdAt", operators = {GT, LT, BETWEEN}),
    @Filter(field = "owner.name", operators = {EQ, LIKE}),          // imbrication supportée
    @Filter(field = "lines.content", operators = {LIKE}, rg = "RG-ENTITE-042"),
})
```

Un filtre **non déclaré** → `400` (avec le champ fautif dans la réponse). Pour un **endpoint custom** (autre
que le `search` standard) qui filtre, porte `@AllowedFilters({...})` **sur la méthode** et valide via
`FilterValidator`.

- `field` accepte l'**imbrication** (`owner.name`, `lines.content`).
- `operators` = liste blanche des opérateurs permis **pour ce champ** (tout autre → `400`).
- `rg = "RG-..."` (optionnel) **documente** : il rattache ce filtre à une **règle métier** (le même code
  que `@BusinessRule`). Purement déclaratif — sert à la traçabilité (le filtre apparaît dans le registre
  `business-rules.json` sous cette règle), n'altère pas le comportement du filtre.

### Requête `POST /search`

La **pagination** et le **tri** sont des query params (scalaires simples) ; seuls les **filtres** typés
vivent dans le corps.

```
POST /api/entites/search?page=1&limit=20&sort=createdAt:desc,name:asc
```

- `page` (défaut `1`, 1-based) · `limit` (défaut `10`, borné à `100`).
- `sort` : format **`champ:direction`** (`asc`/`desc`, **`asc` par défaut** si la direction est omise,
  insensible à la casse). Virgule = enchaînement de champs, param répétable
  (`?sort=createdAt:desc&sort=name` ≡ `?sort=createdAt:desc,name`). Absent → `@DefaultSort` (sinon `id`
  ASC). Le départage final par `id` est **toujours** ajouté → le front n'a jamais à demander `id`.

Corps (facultatif — absent ⇒ aucun filtre) :

```json
{
  "filters": {
    "name":   { "operator": "LIKE", "value": "wid", "options": { "anyAfter": true } },
    "status": { "operator": "IN", "value": ["ACTIF", "EN_PAUSE"] }
  }
}
```

### Réponse

```json
{
  "data": [ /* ... ResponseDto ... */ ],
  "pagination": { "page": 1, "limit": 20, "total": 137, "totalPages": 7 }
}
```

### Opérateurs disponibles

`LIKE` · `EQ` · `NEQ` · `IN` · `NOT_IN` · `GT` · `GTE` · `LT` · `LTE` · `BETWEEN` · `IS_NULL` · `IS_NOT_NULL`

- **Options `LIKE`** : `caseSensitive`, `accentSensitive`, `anyBefore`, `anyAfter`.
- **Options `BETWEEN`** : `includeStart`, `includeEnd` (défaut `true`).
- `EQ` sur une relation `@ManyToOne` → compare l'**uuid_v7**.

> **`search` ou `findAll` ?** `search` (paginé/trié/filtré) couvre la quasi-totalité des cas. Réserve un
> `findAll()` (liste complète, non paginée) aux ressources à volumétrie **petite et bornée** où
> pagination et filtres n'apportent rien — auquel cas tu n'exposes pas `@Expose(SEARCH)`.

---

## Sécurité & erreurs

### Authentification

> Le **moyen de connexion n'est pas figé** par le platform (OIDC, IdP externe, ou autre selon le
> déploiement) — **ce projet ne code pas l'authentification**. L'identité arrive **déjà validée** à
> l'application ; côté projet, tu n'as qu'à exploiter l'utilisateur/rôle courant et à déclarer la
> **politique d'accès** (ci-dessous).

- L'identité de l'appelant (dont le **rôle**) est portée par le token validé ; le rôle est exposé comme
  autorité `ROLE_<ROLE>` pour les règles d'accès.
- La **gestion des comptes** (création, rôles, profil, préférences) relève **en général de l'IdP** : le
  platform n'embarque par défaut **aucune entité `User`**. *(Exception : un projet qui gère lui-même son
  authentification peut tout à fait définir sa propre entité `User` — c'est alors son choix.)*

> **Auth désactivée par défaut — l'appli s'utilise sans connexion.** À la génération,
> `platform.auth.enabled=false` : le back boote **« non connecté »** et **tous les endpoints sont
> ouverts** (chaîne de repli `permitAll`). C'est **voulu** — tes développeurs peuvent exercer les CRUD,
> les tests et la navigation **sans brancher d'IdP**. Les écritures sont alors auditées comme
> `anonymous` (`createdBy`/`updatedBy`).
>
> **Brancher ton IdP (plus tard, quand il est prêt).** Renseigne les 3 variables du `.env`
> (`AUTH_URL`, `AUTH_CLIENT_ID`, `AUTH_CLIENT_SECRET` — noms **génériques**, valables pour n'importe quel
> IdP indépendant) et passe `platform.auth.enabled=true`. Le flux de connexion (BFF, validation JWT,
> cookies) s'active alors **sans une ligne de code** de ta part, et la politique d'accès ci-dessous
> reprend tout son sens (jusque-là, `permitAll` prime).

### Politique d'accès — DSL `SecurityRule`

Par défaut (`SecurityRules.defaults()`) : les chemins d'infra sont ouverts (`/`, `/health/**`,
`/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/.well-known/**`), **tout le reste authentifié**.

Pour surcharger, expose **un bean** `PlatformSecurityRulesProvider` :

```java
@Bean
PlatformSecurityRulesProvider rules() {
    return () -> List.of(
        SecurityRules.rule("/api/public/**", SecurityRules.permit()),
        SecurityRules.rule(HttpMethod.GET, "/api/entites/**", SecurityRules.roles("ADMIN")),
        SecurityRules.rule("/**", SecurityRules.authenticated()));
}
```

Stateless · CSRF désactivé (protégé par `SameSite`) · CORS origines explicites. La promotion de rôle ne
prend effet **qu'au prochain token** (reconnexion nécessaire).

### Erreurs — ProblemDetail (RFC 9457)

Format de réponse d'erreur : `{ type, title, status, detail, instance, logId }`. Une `500` ne révèle
**aucun détail technique** (seulement `logId`, à corréler avec les logs).

| Code | Quand |
|---|---|
| `400` | JSON illisible, filtre non autorisé |
| `401` | anonyme, token invalide/expiré |
| `403` | accès refusé par la politique d'accès (rôle insuffisant) |
| `404` | ressource introuvable |
| `422` | corps valide mais champ invalide (Bean Validation) |
| `429` | trop de requêtes : throttle anti-flood (même requête trop rapprochée) — en-tête `Retry-After` |

### Validation & i18n

Les contraintes Bean Validation (groupes `Create`/`Update`) sont validées par Spring sur les DTOs
(`@Validated(Create.class)`). Les messages sont externalisés :

```properties
# messages.properties (FR) / messages_en.properties (EN)
error.not_found=La ressource demandée est introuvable.
entite.name.not_blank=Le champ name est obligatoire.
entite.name.size=Le champ name ne peut pas dépasser {max} caractères.
# Throttle anti-flood (toujours présent) :
error.throttled=Trop de requêtes. Réessayez dans {0} seconde(s).
```

### Throttle anti-flood (toujours actif)

> Fourni par `platform-back`, **actif par défaut** (débrayable via `platform.throttle.enabled=false`).
> C'est une **barrière de sécurité de base** présente dans toute appli.

Limite une **même requête** (clé `IP + méthode + chemin`) à **un appel toutes les
`platform.throttle.min-interval-ms`** (défaut **200 ms**) via un token-bucket (Bucket4j) ; les seaux
vivent dans un cache Caffeine borné à éviction (indexé par IP). Dépassement → `429` + en-tête
`Retry-After`. Les chemins d'infra (santé, doc API, `/.well-known/**`) sont exemptés.

```bash
THROTTLE_ENABLED=true            # false pour désactiver
THROTTLE_MIN_INTERVAL_MS=200     # intervalle minimal entre deux appels identiques
```

> Ce throttle protège du **flood** (rafales sur une même route). La protection **brute-force du login**
> (tentatives de mot de passe) relève de l'**IdP**, pas du projet.

### Journalisation

**Ce qui est déjà en place (rien à configurer).** Le platform fournit la config `logback-spring.xml`
pilotée par profil et l'enrichissement automatique de chaque ligne :
- Chaque log porte, via le **MDC**, `timestamp`, `level`, `uri`, `correlationId` (header HTTP → MDC),
  `httpStatus`, `userId` — **tu n'as rien à ajouter**, c'est posé par les filtres du platform.
- **Dev** : console lisible + fichier tournant `logs/<app>.log` (10 Mo / 7 j / cap 200 Mo, archives
  `.log.gz`). **Prod** : JSON structuré (logstash-logback-encoder) sur stdout → ELK/Loki/Grafana.
- Niveaux pilotés par `application.yml` (`logging.level.*`) et `LOG_LEVEL` ; `debug` ignoré en prod.
- Les logs **remontés par le front** (`POST /api/logs/client`, utilisé par la couche) entrent dans la
  **même chaîne** avec le même `correlationId`/`userId` → un incident front se retrouve dans les mêmes logs.

**Ajouter tes logs (même logger, même format).** Tu réutilises simplement un logger SLF4J — le format et
les champs MDC sont appliqués automatiquement. Jamais de `System.out.println` (interdit par Checkstyle).

```java
@Slf4j                                                  // Lombok : fournit le champ `log`
@Service
public class EntiteService extends BaseCrudService<...> {
    public EntiteDetailDto create(EntiteCreateDto dto) {
        log.info("création entite name={}", dto.name());   // {} = placeholder (pas de concaténation)
        try {
            // ...
        } catch (SomeException e) {
            log.error("échec création entite", e);          // exception TOUJOURS en dernier argument
            throw e;
        }
    }
}
```

Sans Lombok : `private static final Logger log = LoggerFactory.getLogger(EntiteService.class);`.
**Niveaux** : `error` (incident), `warn` (anormal récupérable), `info` (événement métier notable),
`debug` (diagnostic dev). N'ajoute **ni** timestamp, **ni** user, **ni** correlationId à la main : ils sont
déjà dans le MDC, donc dans chaque ligne et dans le JSON de prod.

---

# Frontend (Nuxt 4 / Vue 3, SPA)

## Structure

Le front est une **SPA Nuxt 4 / Vue 3** (`ssr: false`). Pas de SEO : nginx sert le statique, Spring
reste le back (API `/api`).

### La couche `@platform/front`

`@platform/front` est une **Nuxt Layer**. Le projet l'active dans `nuxt.config.ts` :

```ts
export default defineNuxtConfig({
  extends: ['@platform/front'],
  ssr: false,
  modules: ['@nuxt/eslint', '@vite-pwa/nuxt'],
})
```

Il **hérite** alors — par auto-import, sans rien recopier — du layout, des composants `Platform*`, des
composables (`useAuth`), du store de notifications, du middleware d'auth global, du client HTTP `$api`,
des 3 thèmes Vuetify (`platform-*`) et des namespaces i18n de base.

### Arborescence du projet (Nuxt 4 : code sous `app/`)

```
frontend/
  nuxt.config.ts          extends ['@platform/front'] · ssr:false · modules projet
  tsconfig.json           references vers .nuxt/tsconfig.*.json
  eslint.config.mjs       @nuxt/eslint + ban des imports ../
  vitest.config.ts        env nuxt (cf. tests)
  i18n/locales/{fr,en}/   namespaces i18n du projet
  platform/               ← la couche @platform/front (NE PAS TOUCHER — hors de ton périmètre)
  app/                    ← srcDir Nuxt 4 (TON code)
    app.config.ts         appName · version · navItems · legalLinks (surcharge la couche)
    app.vue               <NuxtLayout><NuxtPage/></NuxtLayout>
    pages/                routing par fichiers
    domains/              domaines CRUD (un dossier <x>/index.ts par domaine)
    services/             services métier (extends AbstractService)
    components/           tes composants
```

> Le dossier **`platform/`** est la couche `@platform/front` (layer locale). C'est une **zone « ne pas
> toucher »** : ton code vit dans `app/`. Tu n'importes jamais depuis `platform/` directement — tout est
> auto-importé par l'`extends`.

### `app.config.ts` — configuration de l'app

Surcharge (fusion profonde) les valeurs par défaut de la couche. Lu par le layout et les composants
`Platform*` via `useAppConfig().platform`.

```ts
export default defineAppConfig({
  platform: {
    appName: 'test',
    version: '0.1.0',
    navItems: [
      { label: 'nav.home', to: '/', icon: 'mdi-home' },            // label = clé i18n · icon = icône MDI
      // Un item porteur de `children` (un seul niveau) devient un GROUPE : il n'a pas de `to`, il
      // ouvre un sous-menu en « drill-down » (la nav glisse pour n'afficher que ce dossier + un
      // retour « ← ». Pratique pour regrouper sans surcharger le menu racine.
      {
        label: 'nav.dossiers',
        icon: 'mdi-folder-outline',
        children: [
          { label: 'nav.dossiersActifs', to: '/dossiers/actifs' },
          { label: 'nav.dossiersArchives', to: '/dossiers/archives' },
        ],
      },
    ],
    legalLinks: [
      { label: 'legal.mentions', to: '/mentions-legales' },
      { label: 'legal.privacy', to: '/confidentialite' },
    ],
  },
})
```

> **Item simple vs groupe** : un item avec `to` est un lien direct ; un item avec `children` est un
> **groupe** (sans `to`) qui ouvre son sous-menu par glissement. Un seul niveau de sous-menu est géré.

### Routing — par fichiers + middleware global

`app/pages/` = routing par fichiers (pas de table de routes). **Fermé par défaut** : le middleware
global `auth.global.ts` (couche) protège **toutes** les pages, sauf celles marquées publiques :

```ts
// app/pages/contact.vue
definePageMeta({ public: true })
```

Pages générées : `index.vue` (dashboard protégé), `welcome.vue` (`public`, bouton de connexion),
`[...slug].vue` (route attrape-tout — le middleware global `not-found` de la couche redirige toute URL
inconnue vers `/` et la signale au back via `POST /api/logs/client`), pages légales (`public`).

**Enveloppe de page (`bare`).** Le layout enveloppe par défaut le contenu routé dans un **panneau plein
hauteur** (surface arrondie `.platform-page-panel`), pour la cohérence du shell « panneaux flottants ».
Une page qui porte **ses propres cartes** (dashboard, tuiles) s'en exclut pour occuper toute la surface :

```ts
// app/pages/index.vue — page « nue » (sans panneau d'enveloppe)
definePageMeta({ bare: true })
```

### Imports

Auto-import Nuxt (composants, composables, utils) → quasi aucun import relatif. Alias : `~/` et `@/` →
`app/` · `~~/` et `@@/` → racine · `#imports` (API auto-importée). **Les imports `../` sont interdits**
(règle ESLint).

---

## Données & services

### Le client HTTP `$api`

Fourni par la couche (plugin `01.api`), basé sur **ofetch** (`$fetch`). Caractéristiques :

- `baseURL` = `/api`, `credentials: 'include'` → envoie les **credentials** (cookies) de session.
- Sur **401** : une tentative de **rafraîchissement silencieux** de la session puis rejeu de la requête ;
  sinon redirection vers la connexion. Retry réseau hors 4xx.
- Renvoie **directement la donnée typée** (ofetch parse — pas d'objet `{ data }` à déballer).

On l'utilise rarement en direct : on passe par un **service**.

### Écrire un service — `AbstractService`

Les services vivent dans `app/services/` et héritent de `AbstractService` (auto-importé via `#imports`) :

```ts
import { AbstractService } from '#imports'

export interface IEntite {
  id: string
  name: string
  quantity: number
}
export interface IEntiteCreateDto {
  name: string
  quantity: number
}

class EntiteService extends AbstractService {
  protected basePath = '/entites'

  // méthodes protégées dispo : search, findOne, create, update, remove, get, head, post, patch, delete
  public list(filters: ISearchRequest) {
    return this.search<ISearchResponse<IEntite>>(filters)
  }
  public createEntite(dto: IEntiteCreateDto) {
    return this.create<IEntite, IEntiteCreateDto>(dto)
  }
}

export const entiteService = new EntiteService()
```

**Les composants n'appellent que les méthodes `public`** des services (jamais `$api` directement).

```vue
<script setup lang="ts">
const entites = ref<IEntite[]>([])
onMounted(async () => {
  const res = await entiteService.list({ pagination: { page: 1, limit: 20 } })
  entites.value = res.data
})
</script>
```

### Conventions

- Interfaces TS de données : préfixe **`I`** (`IEntite`, `IEntiteCreateDto`).
- Fichiers : `kebab-case` ; services : `entite-service.ts` exportant `entiteService`.
- Pas de cache front (le cache vit côté back, Caffeine). Données métier → composables locaux.

### Composants de la couche (`Platform*`)

Auto-importés (préfixe `Platform`), bâtis sur les composants Vuetify. Disponibles : `<PlatformSidebar>`
(tiroir overlay mobile) · `<PlatformSidebarContent>` (sidebar à deux panneaux : nav drill-down + encart personnalisable) ·
`<PlatformHeader>` (sélecteurs + notifications + menu avatar, sans recherche) · `<PlatformFooter>` ·
`<PlatformUserMenu>` · `<PlatformUserAvatar>` · `<PlatformThemeSelector>` · `<PlatformLanguageSelector>` ·
`<PlatformNotificationPanel>` · `<PlatformToastContainer>` · `<PlatformWatermark>`.

Tu construis tes propres composants par-dessus (un composant = un fichier, `PascalCase.vue`).

### Le shell « panneaux flottants » & la zone personnalisable

Le `layout` par défaut (fourni par la couche) est un **shell « panneaux flottants »** : la sidebar et la
colonne de contenu sont des surfaces arrondies posées sur un fond teinté. Tu n'as **rien à câbler** — toute
page en hérite. La sidebar tient en **deux panneaux** : (1) le menu (marque + navigation drill-down,
alimentée par `navItems`) ; (2) un **encart séparé, personnalisable**.

Cet encart est un **slot `extra`** : par défaut il affiche un placeholder pointillé, mais tu y mets **ce que
tu veux** (un graphe métier, des raccourcis, un résumé d'activité…). Tu le remplis en fournissant le slot au
composant de sidebar — typiquement depuis ton propre layout qui réutilise les composants de la couche :

```vue
<!-- exemple : un graphe d'évolution dans l'encart de la sidebar -->
<PlatformSidebarContent :app-name="appName" :nav-items="navItems">
  <template #extra>
    <MonGrapheEvolution />   <!-- ton composant métier -->
  </template>
</PlatformSidebarContent>
```

L'encart **remplit tout l'espace restant** sous le menu (idéal pour un graphe ou un panneau d'activité).

> En **mode pro**, la couche est inlinée dans `frontend/platform/` : tu peux y éditer directement le
> contenu de l'encart (zone « ne pas toucher » par défaut, mais c'est ton code une fois inliné).
> L'**identité et la déconnexion** ne vivent pas dans la sidebar : elles sont dans le menu de l'avatar (header).

---

## Auth, i18n & thèmes

### Auth — état & garde (fourni par la couche)

> Le **moyen de connexion n'est pas figé** (cf. [Sécurité](#authentification)) — **tu n'écris pas de code
> d'auth**. La couche fournit l'**état d'authentification** et une **garde globale** ; le détail du flux
> de connexion dépend du déploiement. Côté projet, tu lis l'utilisateur courant et tu protèges/ouvres
> tes pages.

```ts
const { user, isAuthenticated, isLoading, login, logout, reload } = useAuth()
```

- `user` : `{ id, login, email, role } | null` — l'utilisateur courant (chargé au boot).
- `login()` / `logout()` → déclenchent la connexion / déconnexion configurée.
- **Protection des pages** : middleware global (cf. [Structure](#structure)) — **tout est protégé** sauf
  `definePageMeta({ public: true })`.

> **Auth désactivée par défaut (front ouvert).** À la génération, `app.config.ts` pose
> `platform.authEnabled: false` : la garde globale est **court-circuitée** et **toutes les pages sont
> accessibles sans connexion** (miroir du back `platform.auth.enabled=false`). C'est voulu — tes devs
> testent les CRUD dans l'UI sans IdP. Pour activer l'auth : renseigne `AUTH_*` dans `.env`, passe
> `platform.auth.enabled=true` côté back, puis repasse `authEnabled` à `true` (ou supprime la ligne
> pour hériter du défaut couche = `true`).

```vue
<script setup lang="ts">
const { user, isAuthenticated, login } = useAuth()
</script>
<template>
  <div v-if="isAuthenticated">Bonjour {{ user!.login }} ({{ user!.role }})</div>
  <v-btn v-else color="primary" @click="login">Se connecter</v-btn>
</template>
```

`user.role` sert à l'**UX** (afficher/masquer) ; la **vraie** garde reste **côté back** (politique d'accès).

### Notifications & toasts

Store Pinia fourni par la couche : `useNotificationStore()`.

```ts
const notifications = useNotificationStore()
notifications.add({ title: 'Enregistré', level: 'success' }) // info | success | warning | error
```

Le `<PlatformToastContainer>` (déjà dans le layout) affiche les toasts en haut à droite, auto-dismiss 5 s.
Pas de persistance : la file est vidée à la déconnexion.

### i18n — `@nuxtjs/i18n`

FR par défaut, fallback FR, `strategy: 'no_prefix'` (la locale n'apparaît **pas** dans l'URL). Les
traductions vivent dans `i18n/locales/{fr,en}/` ; **tous les `.json` d'une locale sont fusionnés** en un
seul dictionnaire. La couche fournit `common.json`, `errors.json`, `validation.json` ; tu ajoutes les tiens
par feature.

**Une clé = `namespace.chemin`**, où le `namespace` est la **clé racine du JSON** (par convention = le nom
du fichier, mais c'est la clé racine qui compte, pas le nom de fichier) :

```jsonc
// i18n/locales/fr/entite.json          (+ le pendant i18n/locales/en/entite.json)
{
  "entite": {                            // ← namespace = clé racine
    "title": "Entites",
    "count": "{count} entite(s)"         // interpolation : {count}, accolade SIMPLE
  }
}
```

```vue
<script setup lang="ts">const { t } = useI18n()</script>
<template>
  <h1 class="text-h4">{{ t('entite.title') }}</h1>
  <p>{{ t('entite.count', { count: 3 }) }}</p>
</template>
```

- **Aucun texte en dur** — toujours `t('namespace.cle')`. Détection + persistance via cookie `platform_locale`.
- **Toujours en double** — chaque clé existe en `fr/` **et** `en/` (sinon fallback FR).
- **CRUD** — dans un domaine (`definePlatformDomain`), les `titles` et `tableHeaders[].title` sont
  **interprétés comme des clés i18n** (résolues, réactives à la locale) : mets-y une clé
  (`'entite.titles.list'`), pas le libellé en dur.

### Thèmes — Vuetify

3 thèmes Vuetify : `platform-dark` (défaut), `platform-light`, `platform-a11y` (WCAG AAA), définis dans
le `nuxt.config.ts` de la couche (`vuetify.vuetifyOptions.theme`). Le thème actif est géré par
`useTheme()` (Vuetify) et persisté selon les préférences utilisateur. Le sélecteur
`<PlatformThemeSelector>` est déjà câblé.

**Règles de style** (à respecter strictement) :
- **Pas de `px`** : `rem` / `vh` / `%` uniquement (rendu indépendant de la taille de police du
  navigateur). `px` réservé aux cas extrêmes (bordures fines, `max-width`).
- **Aucun style dans les `.vue`** (pas de `<style>` ni d'inline, sauf un éventuel composant générique de
  formatage) : tout le CSS vit dans `app/assets/css/main.css` sous des classes `.platform-*`, qui
  **consomment** les tokens de design (`var(--v-platform-*)`).
- Police **Inter**, **JetBrains Mono** pour le code. Privilégier les **helpers Vuetify** (`d-flex`,
  `pa-4`, grille `v-row`/`v-col`).
- Mobile-first (breakpoints Vuetify sm/md/lg/xl, tiroir sous md), **WCAG 2.1 AA**.

---

# Tests

## Tests backend

Stack : **JUnit 5 · Spring Boot Test · MockMvc · H2 en mémoire** (pas de Docker) · ArchUnit.

> L'authentification est mockée (faux IdP, JWKS local) ; la base est H2 (mode PostgreSQL, schéma
> régénéré par JPA, Flyway désactivé). Aucun conteneur requis.

```bash
cd backend
./mvnw test       # tests
./mvnw verify     # + checkstyle + spotbugs (comme la CI)
```

### La base — `PlatformIntegrationTest`

Toute classe de test d'intégration hérite (directement ou via les batteries ci-dessous) de
`PlatformIntegrationTest`. Elle fournit :

- `mockMvc` (contexte Spring complet, sécurité réelle) ;
- des raccourcis d'authentification à appliquer via `.with(...)` :
  - `asUser()` = rôle `USER`, `asAdmin()` = rôle `ADMIN` (les deux courants) ;
  - **un autre rôle ?** `asRole("MODERATOR")` — `asRole(String)` accepte **n'importe quel rôle** (`asUser()`
    n'est qu'un raccourci de `asRole("USER")`) ;
  - `invalidAuth()` = token invalide → 401 ; sans `.with(...)`, la requête est **anonyme** ;
- un `authMock()` **swappable** (défaut : faux IdP local). Surcharge-le pour brancher l'IdP de
  l'employeur — les batteries ne changent pas.

### La surcouche de confort

`PlatformIntegrationTest` fournit des raccourcis pour écrire des tests métier sans la verbosité de
MockMvc. Tout est `protected`, disponible dans toute sous-classe.

```java
// 1. Requêtes JSON — auth + Content-Type posés en une ligne ; le corps est un JSON brut (lisible) :
getJson("/api/entites/" + id, asUser());                                     // GET (USER)
postJson("/api/entites", "{\"name\":\"gizmo\",\"quantity\":3}", asAdmin());  // POST, corps JSON
patchJson("/api/entites/" + id, "{\"name\":\"maj\"}", asAdmin());            // PATCH
deleteJson("/api/entites/" + id, asAdmin());                                 // DELETE

// 2. Seed H2 — persist(...) insère une entité DIRECTEMENT en base (sans passer par l'API), pour préparer
//    l'état du test ; il renvoie l'entité gérée (son id est renseigné) :
Entite e = persist(new Entite("seed", 5));
String seededId = e.getId().toString();

// 3. Lire la réponse — chaque requête renvoie un `ResultActions` (la réponse + des assertions chaînables).
//    On en extrait des valeurs par JSONPath :
//      • read(resultat, "$.name")  → la propriété `name` du JSON renvoyé ($ = racine, .name = propriété)
//      • idOf(resultat)            → raccourci pour read(resultat, "$.id")
ResultActions created = postJson("/api/entites", "{\"name\":\"gizmo\",\"quantity\":3}", asAdmin())
        .andExpect(status().isCreated());            // on peut enchaîner les assertions MockMvc
String id   = idOf(created);                          // id de l'entité créée
String name = read(getJson("/api/entites/" + id, asUser()), "$.name");   // → "gizmo"

// 4. Assertions ProblemDetail (RFC 9457), en une ligne :
expectProblem(getJson("/api/entites/" + inconnu, asUser()), HttpStatus.NOT_FOUND);     // vérifie le status + $.logId
expectValidationError(postJson("/api/entites", "{\"name\":\"\"}", asAdmin()), "name"); // 422 + 'name' dans $.errors
```

> `read`/`idOf` lisent **dans le corps JSON de la réponse** via un chemin **JSONPath** (`$` = la racine,
> `$.name` = la propriété `name`, `$.data[0].id` = l'`id` du 1ᵉʳ élément d'un tableau `data`). Pratique pour
> récupérer un id renvoyé puis l'utiliser dans la requête suivante (cf. `id` ci-dessus).

### Batterie CRUD — `AbstractCrudTest`

**Idée** : tu hérites de `AbstractCrudTest`, tu fournis **le chemin et un payload valide**, et la batterie
**joue toute seule** un cycle CRUD complet contre ton endpoint et vérifie les codes attendus :
**create `201` → findOne `200` → search `200` → update `200` → remove `204`**. Tu ne réécris pas ces appels.

```java
class EntiteCrudTest extends AbstractCrudTest {
  @Override protected String basePath()        { return "/api/entites"; }              // ta ressource
  @Override protected String validCreateJson() { return "{\"name\":\"gizmo\",\"quantity\":3}"; } // un corps valide
  // Surcharges optionnelles :
  //   validUpdateJson() — corps de l'update (défaut : réutilise le create)
  //   writeAuth()       — auth pour create/update/remove (défaut ADMIN)
  //   readAuth()        — auth pour findOne/search       (défaut USER)
}
```

### Batterie des codes d'erreur — `SecurityTest`

**Idée** : même principe, mais pour les **cas d'erreur**. Tu fournis quelques payloads « volontairement
faux » et la batterie vérifie les statuts : **401** (anonyme / token invalide), **400** (JSON illisible ou
**filtre non autorisé**), **422** (champs invalides), **404** (introuvable). Le **403** est **optionnel**.

```java
class EntiteSecurityTest extends SecurityTest {
  @Override protected String basePath()             { return "/api/entites"; }
  // un corps qui VIOLE la validation (ici name vide) → la batterie attend 422 :
  @Override protected String invalidFieldsJson()    { return "{\"name\":\"\",\"quantity\":3}"; }
  // un filtre NON déclaré dans @Expose(SEARCH, allowedFilters=…) → la batterie attend 400 :
  @Override protected String disallowedFilterJson() { return "{\"filters\":{\"secret\":{\"operator\":\"EQ\",\"value\":\"x\"}}}"; }

  // 403 : testé UNIQUEMENT si tu surcharges privilegedRequest() (sinon ignoré — la politique de rôle est
  // propre à ton projet). Décommente pour activer :
  // @Override protected MockHttpServletRequestBuilder privilegedRequest() { return post("/api/entites"); }
}
```

**Bonnes pratiques** : ne re-teste pas les comportements du platform (les batteries les couvrent) — teste
ta **logique métier** (`@BusinessRule`, règles spécifiques). Une classe par batterie (CRUD séparé de
Security). ArchUnit veille (ex. un Controller ne dépend pas directement d'un Repository).

## Tests frontend

Stack : **Vitest + `@nuxt/test-utils` (environnement `nuxt`) + Vue Testing Library**, happy-dom.

> **On ne teste que le front.** La frontière HTTP (`$api` → endpoints back) et l'identité sont
> **toujours mockées** — jamais de backend ni d'IdP réel.

- Fichiers : `*.test.ts` ou `*.spec.ts`, n'importe où (convention : dossier `test/`).
- Lancer : `cd frontend && npm run test` (ou `npm run test:watch`, `npm run test:coverage`).
- Tout s'importe depuis **`@platform/front/testing`** (import unique).

### Les trois niveaux

**1. Unitaire** — fonctions pures (algorithmes). Pas besoin de Nuxt : `// @vitest-environment node` en tête.

```ts
// @vitest-environment node
import { describe, it, expect } from 'vitest'
import { parseAmount } from '~/utils/parseAmount'

describe('parseAmount', () => {
  it('convertit une virgule', () => expect(parseAmount('12,50')).toBe(12.5))
  it('lève sur entrée invalide', () => expect(() => parseAmount('x')).toThrow())
})
```

> **Quoi tester ?** Pas *toutes* les fonctions — seulement celles qui portent une **logique** : conversion
> de date/montant, parsing, calcul, formatage non trivial, validation. Un simple passe-plat (getter,
> wrapper d'une lib) n'a pas besoin de test.

**2. Intégration (service)** — vérifie qu'un service tape le bon endpoint et **transforme** correctement la
réponse, le back étant stubé via `mockApi`.

```ts
import { describe, it, expect } from 'vitest'
import { mockApi } from '@platform/front/testing'
import { entiteService } from '~/services/entite-service'

it('list() mappe l\'enveloppe en liste typée', async () => {
  mockApi({ 'POST /entites/search': {
    data: [{ id: '1', name: 'gizmo', quantity: 3 }],
    pagination: { page: 1, limit: 20, total: 1, totalPages: 1 },
  } })
  const res = await entiteService.list({ pagination: { page: 1, limit: 20 } })
  expect(res.data).toHaveLength(1)
})
```

> **À utiliser avec parcimonie.** Si le service ne fait que **relayer** la requête (passe-plat, comme
> ci-dessus), ce test ne vérifie quasiment que le mock — peu de valeur. Écris-le **seulement** quand le
> service ajoute de la logique : mapping/agrégation de la réponse, paramètres dérivés, gestion d'erreur
> spécifique. Sinon, va directement au niveau 3 (fonctionnement).

**3. Fonctionnement** — le plus utile : monte une page/un composant et vérifie un **comportement réel**
(rendu **+ interaction utilisateur + effet**), `$api` et l'identité étant mockés.

```ts
import { describe, it, expect, beforeEach } from 'vitest'
import {
  renderSuspended, screen, fireEvent, loginAs, mockApi, buildUser, expectToast,
} from '@platform/front/testing'
import EntitesPage from '~/pages/entites.vue'

describe('page /entites', () => {
  beforeEach(() => loginAs(buildUser({ role: 'ADMIN' })))   // identité authentifiée

  it('affiche la liste reçue du back', async () => {
    mockApi({ 'POST /entites/search': {
      data: [{ id: '1', name: 'gizmo', quantity: 3 }],
      pagination: { page: 1, limit: 20, total: 1, totalPages: 1 },
    } })
    await renderSuspended(EntitesPage)
    expect(await screen.findByText('gizmo')).toBeTruthy()    // findBy = attend l'appel async
  })

  it('crée une entité et notifie au clic sur « Créer »', async () => {
    mockApi({
      'POST /entites/search': { data: [], pagination: { page: 1, limit: 20, total: 0, totalPages: 0 } },
      'POST /entites': { id: '2', name: 'nouveau', quantity: 0 },   // réponse de la création
    })
    await renderSuspended(EntitesPage)

    await fireEvent.click(await screen.findByRole('button', { name: /créer/i }))
    await fireEvent.update(screen.getByLabelText(/nom/i), 'nouveau')
    await fireEvent.click(screen.getByRole('button', { name: /enregistrer/i }))

    await expectToast('success')                              // un toast de succès a été émis
    expect(await screen.findByText('nouveau')).toBeTruthy()   // la liste reflète la création
  })
})
```

> Cible ce niveau sur les **interactions qui comptent** (soumission, validation affichée, garde d'accès,
> état vide vs rempli), pas sur « la page s'affiche ». C'est lui qui attrape les vraies régressions.

### Les helpers (`@platform/front/testing`)

| Helper | Rôle |
|---|---|
| `buildUser(overrides?)` | fixture d'utilisateur (`{ id, login, email, role }`) |
| `mockApi({ 'POST /x/search': … })` | stube des endpoints back (préfixe `/api` auto ; valeur = JSON ou `(event) => …`) |
| `loginAs(user \| null)` | stube l'identité (`GET /auth/me`) ; `null` = anonyme |
| `expectToast(level?, titre?)` | vérifie qu'un toast a été émis |
| `renderSuspended` / `mountSuspended` | monte un composant/page dans un vrai contexte Nuxt |
| `screen` / `fireEvent` / `waitFor` | re-exports Vue Testing Library |

**À retenir** : `getByX` = synchrone (lève si absent) · **`findByX`** = async (attend les données) ·
`queryByX` = `null` si absent. Cibler par rôle : `getByRole('button', { name: '…' })`. En test, locale
`en` par défaut (force `document.cookie = 'platform_locale=fr'` pour cibler du texte FR). La config
(`vitest.config.ts`) est déjà fournie ; `npm run test` passe même sans aucun test (`--passWithNoTests`).
Playwright (E2E vrai navigateur) n'est pas câblé — ajoutable plus tard.

---

# Bonnes pratiques (transverses)

Le fil rouge : **fermé par défaut, ouvert par déclaration explicite.** Une route, une opération CRUD, un
filtre, une page publique, un query param : rien n'existe tant que ce n'est pas **déclaré**. En cas de
doute, déclare le minimum.

## Principes généraux

| ✅ À faire | ❌ À éviter |
|---|---|
| Déclarer explicitement chaque opération (`@Expose`), filtre (`@Expose(SEARCH, allowedFilters=…)`), page publique (`public: true`) | Supposer qu'une opération « marche par défaut » sans l'avoir déclarée |
| Laisser le platform gérer le générique (CRUD, cache, auth, erreurs) | Ré-implémenter ce que `BaseCrudService` / la couche fournissent déjà |
| Coder uniquement la **logique métier** (endpoints `@BusinessRule`, règles spécifiques) | Mélanger métier et plomberie dans les controllers |
| Garder les noms du tableau de nommage (§ ci-dessous) | Inventer des conventions locales |

## Sécurité

- **La vraie garde est côté back** (`PlatformSecurityRulesProvider` + `SecurityRules`). Le rôle affiché
  côté front (`user.role`) ne sert qu'à l'UX — **ne jamais** s'en remettre à lui pour autoriser une action.
- **Aucun secret ni token en JS** : ne stocke jamais d'identifiant/token en `localStorage`/`state`.
- Une `500` ne révèle **aucun détail technique** (seulement `logId`). Ne contourne pas le `ProblemDetail`.
- Le **throttle anti-flood** est toujours actif ; la **protection brute-force du login** relève de l'IdP,
  pas du projet.
- Gestion des comptes/rôles/profil = **IdP** par défaut (pas d'entité `User`) ; un projet **pro** qui gère
  sa propre auth peut faire exception.
- Tu ne codes **pas** l'authentification (moyen de connexion non figé) ; tu déclares la **politique
  d'accès** (`SecurityRules`) et tu exploites le rôle/utilisateur courant.

## Données, validation & migrations

- Contraintes Bean Validation sur l'**entité** (sans `groups`) → recopiées dans les DTOs, `groups`
  ajoutés, par `java tools/DtoGenerator.java <Entité>`. Champ ajouté après coup : reporter le champ **et**
  ses contraintes manuellement dans les DTOs.
- Colonnes `String` : `nullable = false, length = 100` par défaut ; tout assouplissement est explicite.
- **Dev** : le schéma suit les entités (`ddl-auto: update`), pas de migration à écrire. **Prod** : une
  migration Flyway exécutée **ne se modifie jamais** — on en ajoute une nouvelle, générée **depuis les
  entités JPA** (pas de SQL à la main).
- Messages d'erreur/validation **externalisés** (`messages*.properties`), jamais en dur dans le code.

## API & performance

- Toute liste passe par **`POST /ressource/search`** (filtres au corps, `page`/`limit`/`sort` en query
  params). Pas de GET qui filtre via query params, pas de verbe dans l'URI.
- Tri : `sort=champ:asc,champ2:desc` (`asc` par défaut). Ne demande **jamais** `id` côté front : le
  départage par `id` est ajouté automatiquement.
- Le **cache vit côté back** (Caffeine, `@Cacheable`/`@CacheEvict` auto). **Pas de cache front** —
  données métier via composables locaux, re-fetch sur changement de page/tri.
- `search` par défaut (paginé/trié/filtré) ; un `findAll()` simple est réservé aux ressources à
  volumétrie **petite et bornée** (sinon toujours `search`).

## Frontend

- **Pas de `px`** : `rem` / `vh` / `%` (px réservé aux bordures fines, `max-width`).
- **Aucun style dans les `.vue`** (ni `<style>`, ni inline, sauf composant générique de formatage) : le
  CSS vit dans `app/assets/css/main.css` sous des classes `.platform-*` qui consomment les tokens
  `var(--v-platform-*)`. Privilégier les helpers Vuetify (`d-flex`, `pa-4`, `v-row`/`v-col`).
- **Aucun texte en dur** : tout passe par i18n (`t('namespace.cle')`).
- **Imports `../` interdits** (ESLint) : utiliser l'auto-import Nuxt et les alias `~/`, `~~/`, `#imports`.
- Les composants n'appellent **que les méthodes `public`** des services, jamais `$api` directement.
- Un composant = un fichier (`PascalCase.vue`) ; logique réutilisable → composable `use*`.

## Tests

- **Ne re-teste pas les comportements du platform** (les batteries `AbstractCrudTest`/`SecurityTest` les
  couvrent) — teste ta logique métier.
- Une classe par batterie (CRUD séparé de Security). Côté front, la frontière HTTP et l'identité sont
  **toujours mockées** (jamais de backend ni d'IdP réels).

## Conventions de nommage

| Élément | Convention | Exemple |
|---|---|---|
| Classes Java | `PascalCase` | `EntiteService` |
| Interfaces Java (systèmes externes multi-impl. uniquement) | `IPascalCase` | `IPaymentProvider` |
| Interfaces TS (toutes les données) | `IPascalCase` | `IEntite` |
| Méthodes / variables | `camelCase` | `findById`, `userId` |
| Constantes / enums | `UPPER_SNAKE_CASE` | `MAX_RETRY` |
| Fichiers | `kebab-case` | `entite-service.ts` |
| Tables / colonnes BDD | `snake_case` | `created_at` |

Suffixes back : `Service · Repository · Controller · Mapper · CreateDto · UpdateDto · DetailDto/SummaryDto
· Exception · Config`. Endpoints métier custom : `@BusinessRule(code = "RG-RESSOURCE-NUM", …)` obligatoire.

> **Projet en mode pro** : le dossier `platform/` (back et front) est une **zone « ne pas toucher »** —
> c'est la plateforme inlinée, hors de ton périmètre. Tout ton code vit en dehors.
