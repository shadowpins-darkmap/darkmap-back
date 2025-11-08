package com.sp.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Determines which frontend URL to redirect to and which cookie domain to use
 * based on the incoming request. This replaces the previous header-only heuristic
 * so that we can support multiple deployed frontends while keeping cookies scoped
 * to the API domain.
 */
@Slf4j
@Component
public class EnvironmentResolver {

    private final String defaultFrontendUrl;
    private final Set<String> allowedOrigins;
    private final String fallbackCookieDomain;

    public EnvironmentResolver(
            @Value("${frontend.default-url}") String defaultFrontendUrl,
            @Value("${frontend.allowed-origins}") List<String> allowedOrigins,
            @Value("${frontend.cookie-domain:}") String fallbackCookieDomain
    ) {
        this.defaultFrontendUrl = normalize(defaultFrontendUrl).orElseThrow(
                () -> new IllegalArgumentException("frontend.default-url must be provided"));
        this.allowedOrigins = allowedOrigins == null
                ? Set.of()
                : allowedOrigins.stream()
                .map(EnvironmentResolver::normalize)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
        this.fallbackCookieDomain = fallbackCookieDomain == null ? "" : fallbackCookieDomain.trim();
    }

    public EnvironmentConfig resolve(HttpServletRequest request) {
        return resolve(request, null);
    }

    public EnvironmentConfig resolve(HttpServletRequest request, String preferredFrontend) {
        String frontendUrl = selectFrontend(preferredFrontend, request);
        boolean isLocal = isLocal(frontendUrl) || hostIsLocal(request.getServerName());
        String cookieDomain = determineCookieDomain(request, isLocal);
        return new EnvironmentConfig(frontendUrl, cookieDomain, isLocal);
    }

    private String selectFrontend(String preferredFrontend, HttpServletRequest request) {
        return Optional.ofNullable(preferredFrontend)
                .flatMap(EnvironmentResolver::normalize)
                .filter(this::isAllowedOrigin)
                .or(() -> extractOrigin(request.getHeader("Origin")))
                .filter(this::isAllowedOrigin)
                .or(() -> extractOrigin(request.getHeader("Referer")))
                .filter(this::isAllowedOrigin)
                .orElse(defaultFrontendUrl);
    }

    private boolean isAllowedOrigin(String origin) {
        if (origin == null) {
            return false;
        }
        if (isLocal(origin)) {
            return true;
        }
        if (allowedOrigins.isEmpty()) {
            return true;
        }
        return allowedOrigins.contains(normalize(origin).orElse(origin));
    }

    private static Optional<String> extractOrigin(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return Optional.empty();
        }
        try {
            URI uri = new URI(headerValue.trim());
            String scheme = uri.getScheme();
            String authority = uri.getAuthority();
            if (scheme == null || authority == null) {
                return Optional.empty();
            }
            return Optional.of(scheme.toLowerCase(Locale.ROOT) + "://" + authority.toLowerCase(Locale.ROOT));
        } catch (Exception e) {
            log.warn("Failed to parse origin header: {}", headerValue, e);
            return Optional.empty();
        }
    }

    private static Optional<String> normalize(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        String trimmed = url.trim();
        // Remove trailing slashes for stable comparison.
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return Optional.of(trimmed.toLowerCase(Locale.ROOT));
    }

    private String determineCookieDomain(HttpServletRequest request, boolean isLocal) {
        if (isLocal) {
            String host = Optional.ofNullable(request.getServerName()).orElse("localhost");
            return host;
        }
        if (!fallbackCookieDomain.isBlank()) {
            return fallbackCookieDomain;
        }
        return Optional.ofNullable(request.getServerName())
                .filter(host -> !host.isBlank())
                .orElse("localhost");
    }

    private static boolean isLocal(String url) {
        if (url == null) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("localhost") || lower.contains("127.0.0.1");
    }

    private static boolean hostIsLocal(String host) {
        if (host == null) {
            return false;
        }
        String lower = host.toLowerCase(Locale.ROOT);
        return lower.contains("localhost") || lower.contains("127.0.0.1");
    }
}
