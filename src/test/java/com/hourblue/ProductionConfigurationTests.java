package com.hourblue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hourblue.seo.SiteUrlBuilder;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@ActiveProfiles("prod")
@SpringBootTest
class ProductionConfigurationTests {

    @Autowired
    private Environment environment;

    @Autowired
    private SiteUrlBuilder siteUrlBuilder;

    @DynamicPropertySource
    static void productionProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_HOST", () -> "127.0.0.1");
        registry.add("DB_PORT", () -> "3306");
        registry.add("DB_NAME", () -> System.getenv().getOrDefault("TEST_DB_NAME", "hourblue_test"));
        registry.add("DB_USERNAME", () -> System.getenv().getOrDefault("DB_USERNAME", "hourblue_app"));
        registry.add("DB_PASSWORD", () -> System.getenv().getOrDefault("DB_PASSWORD", ""));
        registry.add("DB_SSL_MODE", () -> "DISABLED");
        registry.add("SITE_BASE_URL", () -> "https://hourblue.example");
        registry.add("ADMIN_BOOTSTRAP_EMAIL", () -> "");
        registry.add("ADMIN_BOOTSTRAP_PASSWORD", () -> "");
        registry.add("CLOUDINARY_CLOUD_NAME", () -> "");
        registry.add("CLOUDINARY_API_KEY", () -> "");
        registry.add("CLOUDINARY_API_SECRET", () -> "");
    }

    @Test
    void productionProfileLoadsWithEnvironmentDrivenConfiguration() {
        assertEquals("true", environment.getProperty("server.servlet.session.cookie.secure"));
        assertEquals("never", environment.getProperty("server.error.include-stacktrace"));
        assertEquals("7d", environment.getProperty("spring.web.resources.cache.cachecontrol.max-age"));
        assertEquals("https://hourblue.example/", siteUrlBuilder.canonical("/"));
    }
}
