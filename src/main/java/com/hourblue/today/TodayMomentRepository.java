package com.hourblue.today;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodayMomentRepository extends JpaRepository<TodayMoment, Long> {

    @EntityGraph(attributePaths = {"post", "post.category"})
    Optional<TodayMoment> findByFeatureDate(LocalDate featureDate);
}
