package com.hourblue.subscriber;

import java.util.Locale;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class SubscriptionService {

    private final SubscriberRepository subscriberRepository;
    private final TransactionTemplate transactionTemplate;

    public SubscriptionService(
            SubscriberRepository subscriberRepository,
            PlatformTransactionManager transactionManager) {
        this.subscriberRepository = subscriberRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void subscribe(String email) {
        String normalizedEmail = normalize(email);
        if (subscriberRepository.existsByEmail(normalizedEmail)) {
            return;
        }

        try {
            transactionTemplate.execute(status -> {
                subscriberRepository.saveAndFlush(new Subscriber(normalizedEmail));
                return null;
            });
        } catch (DataIntegrityViolationException exception) {
            if (subscriberRepository.existsByEmail(normalizedEmail)) {
                return;
            }
            throw exception;
        }
    }

    private String normalize(String email) {
        return Objects.requireNonNull(email).trim().toLowerCase(Locale.ROOT);
    }
}
