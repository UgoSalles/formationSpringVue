package platform.common.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import platform.common.exception.FilterNotAllowedException;
import platform.common.exception.FilterRequiredException;
import platform.common.exception.ResourceNotFoundException;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Gestionnaire global d'exceptions. Retourne des réponses au format ProblemDetail (RFC 9457).
 *
 * <p>Exceptions gérées :
 * <ul>
 *   <li>{@link ResourceNotFoundException} → 404</li>
 *   <li>{@link FilterNotAllowedException} → 400, champ/opérateur non autorisé</li>
 *   <li>{@link FilterRequiredException} → 400, filtre obligatoire absent</li>
 *   <li>{@link MethodArgumentNotValidException} → 422 (Unprocessable Entity) avec liste de {@code errors}</li>
 *   <li>Exceptions Spring MVC standard → délégation à {@link ResponseEntityExceptionHandler}</li>
 *   <li>{@link Exception} → 500, aucun détail technique, {@code logId} uniquement</li>
 * </ul>
 *
 * <p>Clés i18n requises dans {@code messages.properties} :
 * <pre>
 * error.not_found=La ressource demandée est introuvable.
 * error.filter_not_allowed=Le filtre sur le champ ''{0}'' n''est pas autorisé.
 * error.filter_operator_not_allowed=L''opérateur {1} n''est pas autorisé pour le champ ''{0}''.
 * error.filter_required=Le filtre sur le champ ''{0}'' est obligatoire.
 * error.validation=Les données envoyées sont invalides.
 * error.internal=Une erreur inattendue s''est produite.
 * </pre>
 */
// Priorité la plus basse : ce filet de sécurité (dont le catch-all @ExceptionHandler(Exception.class) →
// 500) doit être consulté APRÈS tout advice spécifique — platform (throttle) comme projet généré.
// Sans cet ordre, l'ordre entre @ControllerAdvice non ordonnés est indéterministe et le catch-all peut
// masquer un handler ciblé.
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    // ── 404 ───────────────────────────────────────────────────────────────────

    /**
     * Gère {@link ResourceNotFoundException} et retourne un 404.
     *
     * @param ex  exception levée
     * @param req requête HTTP en cours
     * @return 404 avec ProblemDetail i18n
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest req) {

        String logId = logId();
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setType(URI.create("/errors/not-found"));
        problem.setDetail(msg("error.not_found", null, req.getLocale()));
        problem.setInstance(URI.create(req.getRequestURI()));
        problem.setProperty("logId", logId);

        log.debug("Resource not found [logId={}, path={}]: {}", logId, req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    // ── 400 filtre non autorisé ───────────────────────────────────────────────

    /**
     * Gère {@link FilterNotAllowedException} : champ ou opérateur non déclaré
     * dans {@code @AllowedFilters}.
     *
     * @return 400 avec ProblemDetail i18n (paramètres : field, operator)
     */
    @ExceptionHandler(FilterNotAllowedException.class)
    public ResponseEntity<ProblemDetail> handleFilterNotAllowed(
            FilterNotAllowedException ex, HttpServletRequest req) {

        String logId = logId();
        String msgKey = ex.getOperator() != null
                ? "error.filter_operator_not_allowed"
                : "error.filter_not_allowed";

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("/errors/filter-not-allowed"));
        problem.setDetail(msg(msgKey, new Object[]{ex.getField(), ex.getOperator()}, req.getLocale()));
        problem.setInstance(URI.create(req.getRequestURI()));
        problem.setProperty("logId", logId);
        problem.setProperty("field", ex.getField());
        if (ex.getOperator() != null) {
            problem.setProperty("operator", ex.getOperator().name());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // ── 400 filtre obligatoire manquant ───────────────────────────────────────

    /**
     * Gère {@link FilterRequiredException} : filtre marqué {@code required = true}
     * absent de la requête.
     *
     * @return 400 avec ProblemDetail i18n (paramètre : field)
     */
    @ExceptionHandler(FilterRequiredException.class)
    public ResponseEntity<ProblemDetail> handleFilterRequired(
            FilterRequiredException ex, HttpServletRequest req) {

        String logId = logId();
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("/errors/filter-required"));
        problem.setDetail(msg("error.filter_required", new Object[]{ex.getField()}, req.getLocale()));
        problem.setInstance(URI.create(req.getRequestURI()));
        problem.setProperty("logId", logId);
        problem.setProperty("field", ex.getField());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // ── 422 validation des champs ─────────────────────────────────────────────

    /**
     * Surcharge la gestion de {@link MethodArgumentNotValidException} : le corps est syntaxiquement
     * valide mais des champs violent une contrainte (Bean Validation). On retourne <strong>422</strong>
     * (et non 400) pour que le front distingue « requête illisible/illégale » (400) de « champs
     * invalides » (422) et puisse mapper chaque erreur sur son champ de formulaire.
     *
     * @return 422 avec ProblemDetail et propriété {@code errors} (field + message)
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String logId = logId();
        Locale locale = resolveLocale(request);
        String path = resolvePath(request);

        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message", resolveFieldMessage(fe, locale)))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        problem.setType(URI.create("/errors/validation"));
        problem.setDetail(msg("error.validation", null, locale));
        problem.setInstance(URI.create(path));
        problem.setProperty("logId", logId);
        problem.setProperty("errors", errors);

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).headers(headers).body(problem);
    }

    // ── 500 ───────────────────────────────────────────────────────────────────

    /**
     * Filet de sécurité : capture toute exception non gérée et retourne un 500.
     * Aucun détail technique n'est exposé — le {@code logId} permet de retrouver
     * l'erreur dans les logs.
     *
     * @param ex  exception non gérée
     * @param req requête HTTP en cours
     * @return 500 avec ProblemDetail générique et logId
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(
            Exception ex, HttpServletRequest req) {

        String logId = logId();
        log.error("Unexpected error [logId={}]", logId, ex);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setType(URI.create("/errors/internal"));
        problem.setDetail(msg("error.internal", null, req.getLocale()));
        problem.setInstance(URI.create(req.getRequestURI()));
        problem.setProperty("logId", logId);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String msg(String code, Object[] args, Locale locale) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            return code;
        }
    }

    /**
     * Résout le message d'une erreur de champ : tente d'abord les codes Spring
     * (ex : {@code user.nom.not_blank}), sinon utilise le message par défaut.
     */
    private String resolveFieldMessage(FieldError fe, Locale locale) {
        if (fe.getCodes() != null) {
            for (String code : fe.getCodes()) {
                try {
                    return messageSource.getMessage(code, fe.getArguments(), locale);
                } catch (NoSuchMessageException ignored) {
                    // tente le code suivant
                }
            }
        }
        return fe.getDefaultMessage() != null ? fe.getDefaultMessage() : fe.getField();
    }

    /** Retourne le correlationId du contexte de log s'il existe, sinon génère un UUID. */
    private static String logId() {
        String id = MDC.get("correlationId");
        return id != null ? id : UUID.randomUUID().toString();
    }

    private static Locale resolveLocale(WebRequest request) {
        if (request instanceof ServletWebRequest swr) {
            return swr.getRequest().getLocale();
        }
        return Locale.getDefault();
    }

    private static String resolvePath(WebRequest request) {
        if (request instanceof ServletWebRequest swr) {
            return swr.getRequest().getRequestURI();
        }
        return "/";
    }
}
