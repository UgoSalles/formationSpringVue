package platform.common.logging;

import java.util.Map;

/**
 * Charge utile d'un log applicatif émis par le frontend (POST /api/logs/client).
 *
 * <p>DTO interne au platform (appelé par le middleware front), normalisé à la construction plutôt que
 * via Bean Validation : la lib n'embarque pas {@code jakarta.validation} sur son classpath principal.
 *
 * @param level   niveau souhaité (debug|info|warn|error) ; valeur absente ou inconnue → warn
 * @param message message à journaliser ; jamais nul, tronqué au-delà de la longueur maximale
 * @param context contexte libre (ex. chemin de la route introuvable) ; jamais nul
 */
public record ClientLogRequest(String level, String message, Map<String, Object> context) {

    /** Longueur maximale conservée pour le message (au-delà : tronqué). */
    public static final int MAX_MESSAGE_LENGTH = 500;

    public ClientLogRequest {
        if (level == null || level.isBlank()) {
            level = "warn";
        }
        if (message == null || message.isBlank()) {
            message = "(message vide)";
        } else if (message.length() > MAX_MESSAGE_LENGTH) {
            message = message.substring(0, MAX_MESSAGE_LENGTH);
        }
        if (context == null) {
            context = Map.of();
        }
    }
}
