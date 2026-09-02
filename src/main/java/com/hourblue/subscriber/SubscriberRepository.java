package com.hourblue.subscriber;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

    boolean existsByEmail(String email);

    @Modifying
    @Query("delete from Subscriber subscriber where subscriber.email = :email")
    int deleteByEmail(@Param("email") String email);
}
