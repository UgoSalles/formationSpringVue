package platform.auth;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Auto-configuration de l'authentification BFF générique (resource-server JWT + flux OAuth2),
 * pilotée par la configuration ({@link PlatformAuthProperties}) sans couplage à un IdP précis.
 *
 * <p>Fournit, lorsqu'une application web sécurisée est présente et que {@code platform.auth.enabled}
 * n'est pas explicitement {@code false} :
 * <ul>
 *   <li>un {@link JwtDecoder} validant les JWT de l'IdP via JWKS (RS256) ;</li>
 *   <li>une {@link SecurityFilterChain} stateless en resource server, lisant le token depuis le
 *       cookie {@code access_token} ({@link CookieBearerTokenResolver}) ;</li>
 *   <li>le contrôleur BFF {@link AuthController} (login / callback / refresh / logout / me).</li>
 * </ul>
 *
 * <p>Débrayable intégralement via {@code platform.auth.enabled=false} (cas par défaut en pro tant que
 * l'IdP n'est pas configuré).
 */
@AutoConfiguration(before = ServletWebSecurityAutoConfiguration.class)
@ConditionalOnClass({SecurityFilterChain.class, JwtDecoder.class})
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "platform.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PlatformAuthProperties.class)
public class AuthAutoConfiguration {

    /**
     * Décodeur JWT pointé sur le JWKS de l'IdP. Valide la signature RS256 et l'expiration
     * (l'IdP n'émet ni {@code iss} ni {@code aud}, donc pas de validateur correspondant).
     *
     * @param props configuration auth platform
     * @return décodeur JWT
     */
    @Bean
    public JwtDecoder platformJwtDecoder(PlatformAuthProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withJwkSetUri(props.jwksUri())
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator()));
        return decoder;
    }

    /**
     * Convertit le claim {@code role} (ex : {@code ADMIN}) en autorité {@code ROLE_ADMIN}.
     *
     * @return convertisseur d'authentification JWT
     */
    @Bean
    public JwtAuthenticationConverter platformJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            List<GrantedAuthority> authorities = role == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + role));
            return authorities;
        });
        return converter;
    }

    /**
     * Résolveur de token lisant le cookie {@code access_token} (repli sur l'en-tête Authorization).
     *
     * @return résolveur de bearer token
     */
    @Bean
    public BearerTokenResolver platformBearerTokenResolver() {
        return new CookieBearerTokenResolver();
    }

    /**
     * Chaîne de sécurité : stateless, resource server JWT, règles d'accès (défaut ou surcharge projet).
     * CSRF désactivé : protection assurée par l'attribut {@code SameSite} des cookies.
     *
     * @param http             builder de sécurité
     * @param decoder          décodeur JWT
     * @param converter        convertisseur d'autorités
     * @param resolver         résolveur de token (cookie)
     * @param rulesProvider    fournisseur de règles optionnel (surcharge projet)
     * @return chaîne de filtres de sécurité
     * @throws Exception si la configuration échoue
     */
    @Bean
    public SecurityFilterChain platformSecurityFilterChain(
            HttpSecurity http,
            JwtDecoder decoder,
            JwtAuthenticationConverter converter,
            BearerTokenResolver resolver,
            ObjectProvider<PlatformSecurityRulesProvider> rulesProvider) throws Exception {

        List<SecurityRule> rules = rulesProvider.getIfAvailable() != null
            ? rulesProvider.getObject().rules()
            : SecurityRules.defaults();

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                for (SecurityRule rule : rules) {
                    var matcher = rule.method() != null
                        ? auth.requestMatchers(rule.method(), rule.path())
                        : auth.requestMatchers(rule.path());
                    switch (rule.access()) {
                        case SecurityRule.PermitAll ignored     -> matcher.permitAll();
                        case SecurityRule.Authenticated ignored -> matcher.authenticated();
                        case SecurityRule.RequireRoles r        -> matcher.hasAnyRole(r.roles());
                    }
                }
            })
            .oauth2ResourceServer(oauth2 -> oauth2
                .bearerTokenResolver(resolver)
                .jwt(jwt -> jwt.decoder(decoder).jwtAuthenticationConverter(converter)));

        return http.build();
    }

    /**
     * Client REST partagé pour les appels confidentiels vers l'IdP.
     *
     * @return client REST
     */
    @Bean
    public RestClient platformAuthRestClient() {
        return RestClient.create();
    }

    /**
     * Client IdP générique (échange de code, refresh, logout, proxy profil).
     *
     * @param props      configuration auth platform
     * @param restClient client REST
     * @return client IdP
     */
    @Bean
    public AuthClient authClient(PlatformAuthProperties props, RestClient restClient) {
        return new AuthClient(props, restClient);
    }

    /**
     * Contrôleur BFF d'authentification.
     *
     * @param props  configuration auth platform
     * @param client client IdP
     * @return contrôleur d'authentification
     */
    @Bean
    public AuthController authController(PlatformAuthProperties props, AuthClient client) {
        return new AuthController(props, client);
    }
}
