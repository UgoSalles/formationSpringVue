# test-frontend — Onboarding (pro, standalone)

Nuxt 4 (Vue 3, SPA) · Vuetify. La couche plateforme est **inlinée** en layer local sous `platform/`
(**ne pas éditer**). Repo **autonome** : aucune dépendance à un registre privé.

## Démarrage (localhost, sans docker)
Le back doit tourner sur `localhost:8080` (cf. `test-backend`). Le serveur de dev proxie `/api`
vers lui (devProxy Nuxt) → same-origin, pas de CORS.
```bash
npm install
npm run dev
```

## À faire en premier
- [ ] Créer les pages dans `app/pages/` et un service par domaine dans `app/services/`.
- [ ] Déclarer les domaines CRUD dans `app/domains/<x>/index.ts` (`definePlatformDomain`).
