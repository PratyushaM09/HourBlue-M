package com.hourblue.seo;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SiteUrlBuilder {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    private final String baseUrl;

    public SiteUrlBuilder(@Value("${hourblue.site-base-url:" + DEFAULT_BASE_URL + "}") String baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    public String canonical(String path) {
        return baseUrl + normalizePath(path);
    }

    public String canonical(String path, int page) {
        String canonical = canonical(path);
        return page > 0 ? canonical + "?page=" + page : canonical;
    }

    private String normalizeBaseUrl(String value) {
        String normalized = value == null || value.isBlank() ? DEFAULT_BASE_URL : value.strip();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        URI uri = URI.create(normalized);
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("SITE_BASE_URL must be an absolute HTTP or HTTPS URL.");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("SITE_BASE_URL must include a host.");
        }
        return normalized;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "/";
        }
        String normalized = path.strip();
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }
}
