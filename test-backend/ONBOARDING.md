# test-backend — Onboarding (pro, standalone)

Spring Boot · Java 21 · PostgreSQL. Le code partagé de la plateforme est **inliné** sous
`src/main/java/platform/` (**ne pas éditer**, hors périmètre projet). Repo **autonome** : aucune
dépendance à un registre privé.

## Démarrage (localhost, sans docker)
1. Une base PostgreSQL joignable sur `localhost:5432` (base `test`). Ex. rapide via docker :
   ```bash
   docker run --name pg -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=test -p 5432:5432 -d postgres
   ```
2. Ajuster `backend/.env` si besoin (`DB_*`). spring-dotenv le charge au démarrage — **pas besoin de
   sourcer** le fichier.
3. Lancer :
   ```bash
   ./mvnw spring-boot:run
   ```

L'auth (MDC) est **désactivée par défaut** (`platform.auth.enabled=false`) : le back boote « non
connecté ». La brancher plus tard en renseignant `AUTH_*` dans `.env` puis `platform.auth.enabled=true`.

## À faire en premier
- [ ] **Renommer le package** placeholder `com.example.test` → votre package réel (`src/main/java/…`, `pom.xml`).
- [ ] Créer les entités métier, puis `java tools/DtoGenerator.java <Entité>` (depuis ce repo).

## Tests
```bash
mvn test    # H2 en mémoire, aucun Docker requis
```
