package com.hourblue.post;

import java.util.List;
import java.util.Optional;

import com.hourblue.category.Category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findBySlug(String slug);

    @EntityGraph(attributePaths = "category")
    Optional<Post> findWithCategoryById(Long id);

    @EntityGraph(attributePaths = "category")
    Page<Post> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<Post> findBySlugAndStatus(String slug, PostStatus status);

    @EntityGraph(attributePaths = "category")
    Page<Post> findAllByStatusOrderByPublishedAtDesc(PostStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<Post> findFirstByStatusOrderByPublishedAtDesc(PostStatus status);

    List<SlugOnly> findAllProjectedByStatusOrderBySlugAsc(PostStatus status);

    @EntityGraph(attributePaths = "category")
    Page<Post> findAllByCategoryAndStatusOrderByPublishedAtDesc(
            Category category,
            PostStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Post> findAllByMoodAndStatusOrderByPublishedAtDesc(Mood mood, PostStatus status, Pageable pageable);

    interface SlugOnly {

        String getSlug();
    }
}
