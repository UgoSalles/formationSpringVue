package com.example.test;

import org.junit.jupiter.api.Test;
import platform.test.PlatformIntegrationTest;

/**
 * Smoke test : démarre le contexte applicatif complet via le harness platform — base H2 en mémoire,
 * JWKS mock jouant MDC et sécurité réelle. Vérifie que la configuration boote.
 *
 * <p>Modèle pour tester une ressource CRUD : créer une classe étendant {@code AbstractCrudTest}
 * (fourni par platform-back) et fournir le chemin de base et les payloads. Helpers d'authentification
 * disponibles via {@code TestTokens} et le cookie {@code accessToken(...)}.
 */
class SmokeTest extends PlatformIntegrationTest {

    @Test
    void contextLoads() {
    }
}
