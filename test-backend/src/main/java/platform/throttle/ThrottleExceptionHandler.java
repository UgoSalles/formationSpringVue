package platform.throttle;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit {@link RequestThrottledException} en {@code 429 Too Many Requests} (ProblemDetail RFC 9457 +
 * en-tête {@code Retry-After}), au même format que {@code GlobalExceptionHandler}.
 *
 * <p>Clé i18n attendue (sinon clé brute) : {@code error.throttled=Trop de requêtes. Réessayez dans {0} seconde(s).}
 *
 * <p>Priorité haute explicite : un advice spécifique doit être consulté avant le catch-all
 * {@code @ExceptionHandler(Exception.class)} du {@code GlobalExceptionHandler} (sinon, à égalité d'ordre,
 * le départage entre advices est indéterministe et le 500 générique peut l'emporter).
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ThrottleExceptionHandler {

    private final MessageSource messageSource;

    public ThrottleExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Gère le throttle anti-flood.
     *
     * @param ex  exception levée par {@link ThrottleInterceptor}
     * @param req requête HTTP en cours
     * @return 429 avec ProblemDetail i18n et en-tête {@code Retry-After}
     */
    @ExceptionHandler(RequestThrottledException.class)
    public ResponseEntity<ProblemDetail> handleThrottled(RequestThrottledException ex, HttpServletRequest req) {
        long retryAfter = ex.getRetryAfterSeconds();
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
        problem.setType(URI.create("/errors/throttled"));
        problem.setDetail(msg("error.throttled", new Object[]{retryAfter}, req.getLocale()));
        problem.setInstance(URI.create(req.getRequestURI()));
        problem.setProperty("logId", logId());
        problem.setProperty("retryAfter", retryAfter);

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter))
            .body(problem);
    }

    private String msg(String code, Object[] args, Locale locale) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            return code;
        }
    }

    private static String logId() {
        String id = MDC.get("correlationId");
        return id != null ? id : UUID.randomUUID().toString();
    }
}
