package com.hourblue.post;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findBySlug(String slug);

    Optional<Post> findBySlugAndStatus(String slug, PostStatus status);

    Page<Post> findAllByStatusOrderByPublishedAtDesc(PostStatus status, Pageable pageable);

    Page<Post> findAllByCategorySlugAndStatusOrderByPublishedAtDesc(
            String categorySlug,
            PostStatus status,
            Pageable pageable);
}
