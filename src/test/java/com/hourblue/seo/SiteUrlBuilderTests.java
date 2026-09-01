package com.hourblue.seo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SiteUrlBuilderTests {

    @Test
    void defaultLocalBaseUrlBuildsCanonicalUrl() {
        SiteUrlBuilder siteUrlBuilder = new SiteUrlBuilder("http://localhost:8080");

        assertEquals("http://localhost:8080/", siteUrlBuilder.canonical("/"));
    }

    @Test
    void configuredOverrideBuildsCanonicalUrl() {
        SiteUrlBuilder siteUrlBuilder = new SiteUrlBuilder("https://hourblue.example");

        assertEquals("https://hourblue.example/posts/hello", siteUrlBuilder.canonical("/posts/hello"));
    }

    @Test
    void trailingSlashDoesNotProduceDoubleSlashes() {
        SiteUrlBuilder siteUrlBuilder = new SiteUrlBuilder("https://hourblue.example/");

        assertEquals("https://hourblue.example/categories/design", siteUrlBuilder.canonical("categories/design"));
    }

    @Test
    void pageZeroOmitsPageQueryAndLaterPagesIncludeOnlyPage() {
        SiteUrlBuilder siteUrlBuilder = new SiteUrlBuilder("https://hourblue.example/");

        assertEquals("https://hourblue.example/", siteUrlBuilder.canonical("/", 0));
        assertEquals("https://hourblue.example/?page=2", siteUrlBuilder.canonical("/", 2));
    }
}
