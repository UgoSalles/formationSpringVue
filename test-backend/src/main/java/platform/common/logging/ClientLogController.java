package platform.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

/**
 * Contrôleur de journalisation côté client : permet au frontend de remonter un événement
 * (ex. route introuvable) dans les logs structurés du backend. Le {@code correlationId} et le
 * {@code userId} sont déjà présents dans le contexte de log Logback (cf. {@link RequestLoggingFilter}). Authentifié
 * par défaut (hors chemins publics). Réponse 204 sans corps. La charge utile est normalisée par
 * {@link ClientLogRequest} (niveau et message bornés) — pas de Bean Validation côté lib.
 */
@RestController
@RequestMapping("/logs")
public class ClientLogController {

    private static final Logger LOG = LoggerFactory.getLogger(ClientLogController.class);

    @PostMapping("/client")
    public ResponseEntity<Void> client(@RequestBody ClientLogRequest body) {
        String message = sanitize(body.message());
        String context = sanitize(String.valueOf(body.context()));
        switch (body.level().toLowerCase(Locale.ROOT)) {
            case "debug" -> LOG.debug("[client] {} | context={}", message, context);
            case "info" -> LOG.info("[client] {} | context={}", message, context);
            case "error" -> LOG.error("[client] {} | context={}", message, context);
            default -> LOG.warn("[client] {} | context={}", message, context);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Neutralise les retours chariot pour éviter l'injection de logs (log forging).
     *
     * @param value valeur brute issue du client
     * @return valeur ramenée sur une seule ligne
     */
    private static String sanitize(String value) {
        return value.replaceAll("[\\r\\n]", " ");
    }
}
