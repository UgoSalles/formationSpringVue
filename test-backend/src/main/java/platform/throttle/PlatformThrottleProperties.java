package platform.throttle;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du throttle applicatif anti-flood (première barrière contre le hammering /
 * double-submit / scraping), <strong>distinct</strong> du rate-limiting par plan.
 *
 * <p>Limite une même requête (clé {@code IP:méthode:chemin}) à un appel toutes les
 * {@code min-interval-ms}. <strong>Activé par défaut</strong> (barrière de sécurité de base). La
 * protection brute-force du login proprement dite relève de l'IdP (MDC).
 */
@ConfigurationProperties(prefix = "platform.throttle")
public class PlatformThrottleProperties {

    /** Active le throttle anti-flood. {@code true} par défaut. */
    private boolean enabled = true;

    /** Intervalle minimal entre deux requêtes identiques (même IP + méthode + chemin), en millisecondes. */
    private long minIntervalMs = 200;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getMinIntervalMs() {
        return minIntervalMs;
    }

    public void setMinIntervalMs(long minIntervalMs) {
        this.minIntervalMs = minIntervalMs;
    }
}
