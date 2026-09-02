package com.hourblue.subscriber;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

class SubscriptionServiceTests {

    private final SubscriberRepository subscriberRepository = mock(SubscriberRepository.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final TransactionStatus transactionStatus = mock(TransactionStatus.class);
    private final SubscriptionService subscriptionService =
            new SubscriptionService(subscriberRepository, transactionManager);

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
    }

    @Test
    void subscribeTrimsWhitespace() {
        subscriptionService.subscribe("  user@example.test  ");

        assertSavedEmail("user@example.test");
    }

    @Test
    void subscribeLowercasesUsingRootLocale() {
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr"));
        try {
            subscriptionService.subscribe("I@EXAMPLE.TEST");
        } finally {
            Locale.setDefault(defaultLocale);
        }

        assertSavedEmail("i@example.test");
    }

    @Test
    void newEmailPersists() {
        subscriptionService.subscribe("new@example.test");

        verify(subscriberRepository).saveAndFlush(any(Subscriber.class));
    }

    @Test
    void existingEmailIsIdempotentSuccess() {
        when(subscriberRepository.existsByEmail("existing@example.test")).thenReturn(true);

        assertDoesNotThrow(() -> subscriptionService.subscribe("existing@example.test"));

        verify(subscriberRepository, never()).saveAndFlush(any());
        verify(transactionManager, never()).getTransaction(any(TransactionDefinition.class));
    }

    @Test
    void duplicateRaceIsVerifiedAfterFailedWriteTransactionRollsBack() {
        DataIntegrityViolationException diagnostic =
                new DataIntegrityViolationException("SQLState23000 duplicate email");
        when(subscriberRepository.existsByEmail("race@example.test")).thenReturn(false, true);
        when(subscriberRepository.saveAndFlush(any(Subscriber.class))).thenThrow(diagnostic);

        assertDoesNotThrow(() -> subscriptionService.subscribe("race@example.test"));

        InOrder order = inOrder(subscriberRepository, transactionManager);
        order.verify(subscriberRepository).existsByEmail("race@example.test");
        order.verify(transactionManager).getTransaction(any(TransactionDefinition.class));
        order.verify(subscriberRepository).saveAndFlush(any(Subscriber.class));
        order.verify(transactionManager).rollback(transactionStatus);
        order.verify(subscriberRepository).existsByEmail("race@example.test");
    }

    @Test
    void nonDuplicateDataIntegrityFailureIsNotSwallowed() {
        DataIntegrityViolationException diagnostic =
                new DataIntegrityViolationException("SQLState23000 unrelated failure");
        when(subscriberRepository.existsByEmail("failure@example.test")).thenReturn(false, false);
        when(subscriberRepository.saveAndFlush(any(Subscriber.class))).thenThrow(diagnostic);

        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> subscriptionService.subscribe("failure@example.test"));
        assertSame(diagnostic, exception);
        verify(transactionManager).rollback(transactionStatus);
    }

    @Test
    void unrelatedPersistenceFailureIsNotSwallowed() {
        IllegalStateException failure = new IllegalStateException("repository unavailable");
        when(subscriberRepository.saveAndFlush(any(Subscriber.class))).thenThrow(failure);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> subscriptionService.subscribe("failure@example.test"));
        assertSame(failure, exception);
        verify(transactionManager).rollback(transactionStatus);
    }

    @Test
    void duplicateDiagnosticIsNotReturnedAsAUserFacingResult() {
        DataIntegrityViolationException diagnostic = new DataIntegrityViolationException("SQLState23000");
        when(subscriberRepository.existsByEmail("duplicate@example.test")).thenReturn(false, true);
        when(subscriberRepository.saveAndFlush(any(Subscriber.class))).thenThrow(diagnostic);

        assertDoesNotThrow(() -> subscriptionService.subscribe("duplicate@example.test"));
    }

    @Test
    void unsubscribeNormalizesAndDeletesInTransaction() {
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr"));
        try {
            subscriptionService.unsubscribe("  I@EXAMPLE.TEST  ");
        } finally {
            Locale.setDefault(defaultLocale);
        }

        InOrder order = inOrder(subscriberRepository, transactionManager);
        order.verify(transactionManager).getTransaction(any(TransactionDefinition.class));
        order.verify(subscriberRepository).deleteByEmail("i@example.test");
        order.verify(transactionManager).commit(transactionStatus);
        verify(subscriberRepository, never()).saveAndFlush(any());
    }

    private void assertSavedEmail(String email) {
        ArgumentCaptor<Subscriber> subscriber = ArgumentCaptor.forClass(Subscriber.class);
        verify(subscriberRepository).saveAndFlush(subscriber.capture());
        org.junit.jupiter.api.Assertions.assertEquals(email, subscriber.getValue().getEmail());
    }
}
