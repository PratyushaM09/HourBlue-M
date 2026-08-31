package com.hourblue.today;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.hourblue.category.Category;
import com.hourblue.category.CategoryRepository;
import com.hourblue.post.Post;
import com.hourblue.post.PostRepository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TodayMomentRepositoryTests {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TodayMomentRepository todayMomentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void todayMomentPersistsAndCanBeFoundByDateWithPostCategoryLoaded() {
        Post post = publishedPost("today");
        LocalDate date = LocalDate.of(2026, 9, 1);
        todayMomentRepository.save(new TodayMoment(date, post));
        entityManager.flush();
        entityManager.clear();

        TodayMoment found = todayMomentRepository.findByFeatureDate(date).orElseThrow();

        assertEquals(date, found.getFeatureDate());
        assertEquals(post.getSlug(), found.getPost().getSlug());
        assertTrue(isLoaded(found.getPost()));
        assertTrue(isLoaded(found.getPost().getCategory()));
    }

    @Test
    void featureDateIsUnique() {
        Post first = publishedPost("first");
        Post second = publishedPost("second");
        LocalDate date = LocalDate.of(2026, 9, 1);
        todayMomentRepository.saveAndFlush(new TodayMoment(date, first));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> todayMomentRepository.saveAndFlush(new TodayMoment(date, second)));
    }

    @Test
    void historicalDatesCanCoexistAndReuseTheSamePost() {
        Post post = publishedPost("reused");
        todayMomentRepository.save(new TodayMoment(LocalDate.of(2026, 9, 1), post));
        todayMomentRepository.save(new TodayMoment(LocalDate.of(2026, 9, 2), post));
        entityManager.flush();
        entityManager.clear();

        assertEquals(post.getSlug(), todayMomentRepository.findByFeatureDate(LocalDate.of(2026, 9, 1))
                .orElseThrow()
                .getPost()
                .getSlug());
        assertEquals(post.getSlug(), todayMomentRepository.findByFeatureDate(LocalDate.of(2026, 9, 2))
                .orElseThrow()
                .getPost()
                .getSlug());
    }

    private Post publishedPost(String label) {
        Category category = categoryRepository.save(new Category(label + " " + unique(), label + "-" + unique(), null));
        Post post = new Post(
                category,
                label + "-" + unique(),
                label + " title",
                label + " description",
                "https://cdn.example.com/" + label + ".jpg",
                label + " alt");
        post.publish(Instant.parse("2026-09-01T00:00:00Z"));
        return postRepository.save(post);
    }

    private String unique() {
        return UUID.randomUUID().toString();
    }

    private boolean isLoaded(Object entity) {
        return entityManager.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(entity);
    }
}
