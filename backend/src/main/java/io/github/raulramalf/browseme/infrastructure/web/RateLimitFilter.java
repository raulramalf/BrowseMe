package io.github.raulramalf.browseme.infrastructure.web;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Límite por IP en memoria (Bucket4j): uno estricto para crear análisis y otro
 * holgado para consultarlos (el cliente sondea). Las IPs solo viven en memoria,
 * en un mapa acotado, y nunca se escriben en logs.
 *
 * <p>Limitación conocida: en memoria y por instancia; se puede evadir rotando IPs.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final int submitsPerMinute;
    private final int readsPerMinute;
    private final Map<String, Bucket> buckets = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
                    return size() > MAX_TRACKED_CLIENTS;
                }
            });

    public RateLimitFilter(int submitsPerMinute, int readsPerMinute) {
        this.submitsPerMinute = submitsPerMinute;
        this.readsPerMinute = readsPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean submit = "POST".equals(request.getMethod());
        String key = (submit ? "w:" : "r:") + request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(submit ? submitsPerMinute : readsPerMinute));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            chain.doFilter(request, response);
            return;
        }
        long retryAfter = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", Long.toString(retryAfter));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"type":"urn:browseme:problem:rate-limited","title":"Demasiadas peticiones","status":429,\
                "detail":"Has hecho demasiadas peticiones. Espera un momento y vuelve a intentarlo.",\
                "code":"RATE_LIMITED"}""");
    }

    private static Bucket newBucket(int perMinute) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(perMinute).refillGreedy(perMinute, Duration.ofMinutes(1)).build())
                .build();
    }
}
