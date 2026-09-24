package platform.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Alimente le contexte de log structuré à chaque requête HTTP :
 * {@code correlationId}, {@code userId}, {@code uri}, {@code httpStatus}.
 *
 * <p>Le {@code correlationId} est lu depuis {@code X-Correlation-ID}
 * ou généré si absent, puis réémis dans la réponse.
 *
 * <p>Le contexte est vidé en fin de requête pour éviter les fuites entre threads.
 */
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String CORRELATION_HEADER = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String correlationId = request.getHeader(CORRELATION_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            MDC.put("correlationId", correlationId);
            MDC.put("uri", request.getRequestURI());
            response.setHeader(CORRELATION_HEADER, correlationId);

            chain.doFilter(request, response);

            MDC.put("httpStatus", String.valueOf(response.getStatus()));
            resolveUserId().ifPresent(id -> MDC.put("userId", id));
        } finally {
            MDC.clear();
        }
    }

    private Optional<String> resolveUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                String name = auth.getName();
                return name != null && !name.isBlank() ? Optional.of(name) : Optional.empty();
            }
        } catch (Exception ignored) {
            // Spring Security absent ou non initialisé
        }
        return Optional.empty();
    }
}
