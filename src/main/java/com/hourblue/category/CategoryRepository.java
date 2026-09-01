package com.hourblue.category;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    List<SlugOnly> findAllProjectedByOrderBySlugAsc();

    List<Category> findAllByOrderByNameAsc();

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    interface SlugOnly {

        String getSlug();
    }
}
