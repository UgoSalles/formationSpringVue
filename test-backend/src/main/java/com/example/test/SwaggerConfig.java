package com.example.test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI / Swagger UI.
 *
 * <p>L'authentification se fait via le flux BFF du platform : après connexion, le cookie
 * {@code access_token} (HttpOnly) est envoyé automatiquement par le navigateur sur les appels
 * Swagger (même origine). Aucun schéma de sécurité OAuth2 à déclarer ici.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info().title("test API").version("0.1.0"));
    }
}
