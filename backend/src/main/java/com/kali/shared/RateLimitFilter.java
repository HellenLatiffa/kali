package com.kali.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        return !(
                path.equals("/api/v1/auth/login") && method.equals("POST")
                        || path.equals("/api/v1/auth/register") && method.equals("POST")
                        || path.equals("/api/v1/me/export") && method.equals("POST")
                        || path.equals("/api/v1/me") && method.equals("DELETE")
                        || path.equals("/api/v1/privacy/consents/health-data")
                        && (method.equals("PUT") || method.equals("DELETE"))
                        || isHealthEntryMutation(path, method)
        );
    }

    private static boolean isHealthEntryMutation(String path, String method) {
        return (path.startsWith("/api/v1/periods/")
                || path.equals("/api/v1/periods")
                || path.startsWith("/api/v1/symptom-entries/")
                || path.equals("/api/v1/symptom-entries")
                || path.startsWith("/api/v1/mood-entries/")
                || path.equals("/api/v1/mood-entries"))
                && (method.equals("POST") || method.equals("PUT") || method.equals("DELETE"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Limit limit = limitFor(request.getRequestURI());
        String key = request.getRemoteAddr() + ':' + request.getRequestURI();
        Instant now = Instant.now();
        Window updated = windows.compute(key, (ignored, current) -> {
            if (current == null || !now.isBefore(current.startedAt.plus(limit.window()))) {
                return new Window(now, 1, false);
            }
            if (current.count >= limit.maximum()) {
                return new Window(current.startedAt, current.count, true);
            }
            return new Window(current.startedAt, current.count + 1, false);
        });

        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.entrySet().removeIf(entry ->
                    !now.isBefore(entry.getValue().startedAt.plus(Duration.ofMinutes(15))));
            if (windows.size() > MAX_TRACKED_CLIENTS) {
                writeTooManyRequests(response);
                return;
            }
        }

        if (updated.limited()) {
            writeTooManyRequests(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static Limit limitFor(String path) {
        if (path.equals("/api/v1/auth/register")) {
            return new Limit(5, Duration.ofMinutes(15));
        }
        if (path.equals("/api/v1/auth/login")) {
            return new Limit(10, Duration.ofMinutes(1));
        }
        if (path.equals("/api/v1/me/export") || path.equals("/api/v1/me")) {
            return new Limit(5, Duration.ofMinutes(15));
        }
        if (path.startsWith("/api/v1/periods")
                || path.startsWith("/api/v1/symptom-entries")
                || path.startsWith("/api/v1/mood-entries")) {
            return new Limit(100, Duration.ofMinutes(15));
        }
        return new Limit(10, Duration.ofMinutes(15));
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", "60");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                new ApiError("RATE_LIMITED", "Muitas solicitações. Tente novamente mais tarde."));
    }

    private record Limit(int maximum, Duration window) {
    }

    private record Window(Instant startedAt, int count, boolean limited) {
    }
}
