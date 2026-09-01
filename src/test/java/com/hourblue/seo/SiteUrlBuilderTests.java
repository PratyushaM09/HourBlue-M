package com.hourblue.seo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SiteUrlBuilderTests {

    @Test
    void defaultLocalBaseUrlBuildsCanonicalUrl() {
        SiteUrlBuilder siteUrlBuilder = new SiteUrlBuilder("http://localhost:8080");

        assertEquals("http://localhost:8080/", siteUrlBuilder.canonical("/"));
    }

    @Test
    void validHttpUrlBuildsCanonicalUrl() {
        SiteUrlBuilder siteUrlBuilder = new SiteUrlBuilder("http://hourblue.example");

        assertEquals("http://hourblue.example/posts/hello", siteUrlBuilder.canonical("/posts/hello"));
    }

    @Test
    void validHttpsUrlBuildsCanonicalUrl() {
        SiteUrlBuilder siteUrlBuilder = new SiteUrlBuilder("https://hourblue.example");

        assertEquals("https://hourblue.example/posts/hello", siteUrlBuilder.canonical("/posts/hello"));
    }

    @Test
    void trailingSlashDoesNotProduceDoubleSlashes() {
        SiteUrlBuilder siteUrlBuilder = new SiteUrlBuilder("https://hourblue.example/");

        assertEquals("https://hourblue.example/categories/design", siteUrlBuilder.canonical("categories/design"));
    }

    @Test
    void blankValueFallsBackToLocalDefault() {
        SiteUrlBuilder siteUrlBuilder = new SiteUrlBuilder(" ");

        assertEquals("http://localhost:8080/", siteUrlBuilder.canonical("/"));
    }

    @Test
    void pageZeroOmitsPageQueryAndLaterPagesIncludeOnlyPage() {
        SiteUrlBuilder siteUrlBuilder = new SiteUrlBuilder("https://hourblue.example/");

        assertEquals("https://hourblue.example/", siteUrlBuilder.canonical("/", 0));
        assertEquals("https://hourblue.example/?page=2", siteUrlBuilder.canonical("/", 2));
    }

    @Test
    void missingSchemeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SiteUrlBuilder("hourblue.example"));
    }

    @Test
    void unsupportedSchemeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SiteUrlBuilder("ftp://hourblue.example"));
    }

    @Test
    void missingHostIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SiteUrlBuilder("http:///relative/path"));
    }
}
