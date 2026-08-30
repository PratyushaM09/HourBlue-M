package com.hourblue.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdminRepositoryTests {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void flywayV2CreatedAdminsTableAndJpaMapping() {
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "select count(*) from flyway_schema_history where version = '2' and success = 1",
                        Integer.class));
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "select count(*) from information_schema.tables where table_schema = database() and table_name = 'admins'",
                        Integer.class));
        assertNotNull(entityManagerFactory.getMetamodel().entity(Admin.class));
    }

    @Test
    void adminCanBePersistedAndFoundByEmail() {
        String email = email();
        String passwordHash = fakePasswordHash();
        Admin admin = adminRepository.saveAndFlush(new Admin(email, passwordHash));

        Admin found = adminRepository.findByEmail(email).orElseThrow();

        assertEquals(admin.getId(), found.getId());
        assertEquals(email, found.getEmail());
        assertEquals(passwordHash, found.passwordHash());
        assertTrue(found.isEnabled());
        assertNotNull(found.getCreatedAt());
        assertNotNull(found.getUpdatedAt());
        assertTrue(adminRepository.findByEmail(email()).isEmpty());
    }

    @Test
    void duplicateEmailIsRejectedByDatabaseConstraint() {
        String email = email();
        adminRepository.saveAndFlush(new Admin(email, fakePasswordHash()));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> adminRepository.saveAndFlush(new Admin(email, fakePasswordHash())));
    }

    private String email() {
        return "admin-" + unique() + "@example.test";
    }

    private String fakePasswordHash() {
        return "$2a$10$fakeHashForPersistenceOnly" + unique().replace("-", "");
    }

    private String unique() {
        return UUID.randomUUID().toString();
    }
}
