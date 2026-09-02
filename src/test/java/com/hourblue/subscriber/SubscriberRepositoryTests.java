package com.hourblue.subscriber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SubscriberRepositoryTests {

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void subscriberPersistsWithNormalizedEmailAndTimestamp() {
        String email = "Subscriber-" + unique() + "@Example.TEST";
        SubscriptionService service = new SubscriptionService(subscriberRepository, transactionManager);

        service.subscribe("  " + email + "  ");

        Subscriber subscriber = subscriberRepository.findAll().stream()
                .filter(saved -> saved.getEmail().equals(email.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow();
        assertNotNull(subscriber.getId());
        assertNotNull(subscriber.getCreatedAt());
    }

    @Test
    void emailUniquenessIsEnforcedByMySql() {
        String email = "subscriber-" + unique() + "@example.test";
        subscriberRepository.saveAndFlush(new Subscriber(email));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> subscriberRepository.saveAndFlush(new Subscriber(email)));
    }

    @Test
    void distinctEmailsCanCoexist() {
        String unique = unique();

        subscriberRepository.saveAndFlush(new Subscriber("one-" + unique + "@example.test"));
        subscriberRepository.saveAndFlush(new Subscriber("two-" + unique + "@example.test"));

        assertTrue(subscriberRepository.existsByEmail("one-" + unique + "@example.test"));
        assertTrue(subscriberRepository.existsByEmail("two-" + unique + "@example.test"));
    }

    @Test
    void existsByEmailFindsExactAddress() {
        String email = "subscriber-" + unique() + "@example.test";
        subscriberRepository.saveAndFlush(new Subscriber(email));

        assertTrue(subscriberRepository.existsByEmail(email));
        assertFalse(subscriberRepository.existsByEmail("missing-" + email));
    }

    @Test
    void unsubscribeDeletesOnlyMatchingNormalizedEmail() {
        String unique = unique();
        String removed = "remove-" + unique + "@example.test";
        String retained = "retain-" + unique + "@example.test";
        SubscriptionService service = new SubscriptionService(subscriberRepository, transactionManager);
        subscriberRepository.saveAndFlush(new Subscriber(removed));
        subscriberRepository.saveAndFlush(new Subscriber(retained));

        service.unsubscribe("  " + removed.toUpperCase(Locale.ROOT) + "  ");
        service.unsubscribe("missing-" + retained);

        assertFalse(subscriberRepository.existsByEmail(removed));
        assertTrue(subscriberRepository.existsByEmail(retained));
        assertEquals(1, subscriberRepository.findAll().stream()
                .filter(subscriber -> subscriber.getEmail().contains(unique))
                .count());
    }

    private String unique() {
        return UUID.randomUUID().toString();
    }
}
