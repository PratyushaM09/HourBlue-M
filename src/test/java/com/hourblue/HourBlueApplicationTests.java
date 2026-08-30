package com.hourblue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;

import com.hourblue.category.Category;
import com.hourblue.post.Post;

import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HourBlueApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private Environment environment;

    @Autowired
    private ZoneId applicationZoneId;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void healthIsUp() {
        ResponseEntity<Map> response =
                restTemplate.getForEntity("/actuator/health", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
    }

    @Test
    void envIsNotExposed() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/actuator/env", String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void devIsTheDefaultProfile() {
        assertTrue(Arrays.asList(environment.getDefaultProfiles()).contains("dev"));
    }

    @Test
    void utcIsTheDefaultApplicationTimeZone() {
        assertEquals(ZoneId.of("UTC"), applicationZoneId);
    }

    @Test
    void connectsToTestDatabase() {
        assertEquals(1, jdbcTemplate.queryForObject("SELECT 1", Integer.class));
    }

    @Test
    void flywayV1CreatedContentTables() {
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "select count(*) from flyway_schema_history where version = '1' and success = 1",
                        Integer.class));
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "select count(*) from information_schema.tables where table_schema = database() and table_name = 'categories'",
                        Integer.class));
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "select count(*) from information_schema.tables where table_schema = database() and table_name = 'posts'",
                        Integer.class));
    }

    @Test
    void jpaMetamodelIncludesContentEntities() {
        assertNotNull(entityManagerFactory.getMetamodel().entity(Category.class));
        assertNotNull(entityManagerFactory.getMetamodel().entity(Post.class));
    }
}
