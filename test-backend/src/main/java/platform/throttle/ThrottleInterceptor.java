package platform.throttle;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Throttle anti-flood : limite une même requête (clé {@code IP:méthode:chemin}) à un appel toutes les
 * {@code minIntervalMs}. Dépassement → {@link RequestThrottledException} ({@code 429} + {@code Retry-After}).
 *
 * <p>Implémenté comme un token-bucket de capacité 1 rechargé d'un jeton par intervalle (Bucket4j). Les
 * seaux sont conservés dans un cache Caffeine borné à éviction (clés volatiles indexées par IP).
 */
public class ThrottleInterceptor implements HandlerInterceptor {

    private final long intervalMs;
    private final Cache<String, Bucket> buckets;

    public ThrottleInterceptor(long intervalMs) {
        this.intervalMs = intervalMs;
        this.buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofMinutes(1))
            .build();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String key = request.getRemoteAddr() + ":" + request.getMethod() + ":" + request.getRequestURI();
        Bucket bucket = buckets.get(key, k -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return true;
        }
        long retryAfter = (long) Math.ceil(probe.getNanosToWaitForRefill() / 1_000_000_000.0);
        throw new RequestThrottledException(retryAfter);
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(1)
            .refillGreedy(1, Duration.ofMillis(intervalMs))
            .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
