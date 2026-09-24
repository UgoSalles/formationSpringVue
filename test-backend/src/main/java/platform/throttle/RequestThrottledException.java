package platform.throttle;

/**
 * Levée par {@link ThrottleInterceptor} quand une même requête (IP + méthode + chemin) est rejouée trop
 * vite. Traduite en {@code 429 Too Many Requests} (ProblemDetail + {@code Retry-After}) par
 * {@link ThrottleExceptionHandler}.
 */
public class RequestThrottledException extends RuntimeException {

    private final long retryAfterSeconds;

    public RequestThrottledException(long retryAfterSeconds) {
        super("Requête throttlée (retryAfter=" + retryAfterSeconds + "s)");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /** @return délai conseillé avant nouvelle tentative, en secondes (au moins 1). */
    public long getRetryAfterSeconds() {
        return Math.max(1, retryAfterSeconds);
    }
}
