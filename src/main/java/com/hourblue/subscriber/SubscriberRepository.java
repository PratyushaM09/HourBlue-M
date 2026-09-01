package com.hourblue.subscriber;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

    boolean existsByEmail(String email);
}
