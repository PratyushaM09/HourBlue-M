package com.hourblue.today;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.hourblue.category.Category;
import com.hourblue.post.Post;
import com.hourblue.post.PostRepository;
import com.hourblue.post.PostStatus;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class TodayMomentServiceTests {

    private final TodayMomentRepository todayMomentRepository = mock(TodayMomentRepository.class);
    private final PostRepository postRepository = mock(PostRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-09-01T20:00:00Z"),
            ZoneId.of("Asia/Kolkata"));
    private final TodayMomentService service = new TodayMomentService(todayMomentRepository, postRepository, clock);

    @Test
    void publishedPostCanBeAssignedToNewDate() {
        LocalDate date = LocalDate.of(2026, 9, 2);
        Post post = publishedPost(5L, "published");
        when(postRepository.findById(5L)).thenReturn(Optional.of(post));
        when(todayMomentRepository.findByFeatureDate(date)).thenReturn(Optional.empty());
        when(todayMomentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TodayMoment saved = service.assign(date, 5L);

        assertEquals(date, saved.getFeatureDate());
        assertEquals(post, saved.getPost());
        verify(todayMomentRepository).saveAndFlush(saved);
    }

    @Test
    void missingDraftAndArchivedPostsAreRejected() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());
        assertEquals(
                "Selected post was not found.",
                assertThrows(InvalidTodayMomentException.class, () -> service.assign(LocalDate.of(2026, 9, 2), 1L))
                        .getMessage());

        when(postRepository.findById(2L)).thenReturn(Optional.of(draftPost(2L, "draft")));
        assertEquals(
                "Selected post must be published.",
                assertThrows(InvalidTodayMomentException.class, () -> service.assign(LocalDate.of(2026, 9, 2), 2L))
                        .getMessage());

        when(postRepository.findById(3L)).thenReturn(Optional.of(archivedPost(3L, "archived")));
        assertEquals(
                "Selected post must be published.",
                assertThrows(InvalidTodayMomentException.class, () -> service.assign(LocalDate.of(2026, 9, 2), 3L))
                        .getMessage());
    }

    @Test
    void sameDateReplacesAssignmentWithoutCreatingAnotherRow() {
        LocalDate date = LocalDate.of(2026, 9, 2);
        Post oldPost = publishedPost(4L, "old");
        Post newPost = publishedPost(5L, "new");
        TodayMoment existing = new TodayMoment(date, oldPost);
        when(postRepository.findById(5L)).thenReturn(Optional.of(newPost));
        when(todayMomentRepository.findByFeatureDate(date)).thenReturn(Optional.of(existing));
        when(todayMomentRepository.saveAndFlush(existing)).thenReturn(existing);

        TodayMoment saved = service.assign(date, 5L);

        assertEquals(date, saved.getFeatureDate());
        assertEquals(newPost, saved.getPost());
        verify(todayMomentRepository).saveAndFlush(existing);
    }

    @Test
    void saveAndFlushDataIntegrityViolationIsTranslatedSafely() {
        LocalDate date = LocalDate.of(2026, 9, 2);
        Post post = publishedPost(5L, "published");
        when(postRepository.findById(5L)).thenReturn(Optional.of(post));
        when(todayMomentRepository.findByFeatureDate(date)).thenReturn(Optional.empty());
        when(todayMomentRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("SQLState23000"));

        InvalidTodayMomentException exception = assertThrows(
                InvalidTodayMomentException.class,
                () -> service.assign(date, 5L));

        assertEquals("Today's Moment could not be saved.", exception.getMessage());
    }

    @Test
    void eligiblePostsArePublishedPostsOnlyNewestFirst() {
        Post post = publishedPost(5L, "published");
        when(postRepository.findAllByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(post)));

        assertEquals(List.of(post), service.eligiblePosts());
    }

    @Test
    void explicitPublishedAssignmentWins() {
        Post explicit = publishedPost(5L, "explicit");
        when(todayMomentRepository.findByFeatureDate(LocalDate.of(2026, 9, 2)))
                .thenReturn(Optional.of(new TodayMoment(LocalDate.of(2026, 9, 2), explicit)));

        assertEquals(Optional.of(explicit), service.resolveTodayMoment());
        verify(postRepository, never()).findFirstByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED);
    }

    @Test
    void missingExplicitAssignmentUsesNewestPublishedFallback() {
        Post fallback = publishedPost(5L, "fallback");
        when(todayMomentRepository.findByFeatureDate(LocalDate.of(2026, 9, 2))).thenReturn(Optional.empty());
        when(postRepository.findFirstByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED))
                .thenReturn(Optional.of(fallback));

        assertEquals(Optional.of(fallback), service.resolveTodayMoment());
    }

    @Test
    void archivedExplicitAssignmentUsesFallback() {
        Post fallback = publishedPost(5L, "fallback");
        when(todayMomentRepository.findByFeatureDate(LocalDate.of(2026, 9, 2)))
                .thenReturn(Optional.of(new TodayMoment(LocalDate.of(2026, 9, 2), archivedPost(6L, "archived"))));
        when(postRepository.findFirstByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED))
                .thenReturn(Optional.of(fallback));

        assertEquals(Optional.of(fallback), service.resolveTodayMoment());
    }

    @Test
    void noExplicitAssignmentAndNoFallbackReturnsEmpty() {
        when(todayMomentRepository.findByFeatureDate(LocalDate.of(2026, 9, 2))).thenReturn(Optional.empty());
        when(postRepository.findFirstByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertTrue(service.resolveTodayMoment().isEmpty());
    }

    @Test
    void todayUsesInjectedClockZone() {
        service.resolveTodayMoment();

        ArgumentCaptor<LocalDate> date = ArgumentCaptor.forClass(LocalDate.class);
        verify(todayMomentRepository).findByFeatureDate(date.capture());
        assertEquals(LocalDate.of(2026, 9, 2), date.getValue());
    }

    private Post draftPost(Long id, String slug) {
        Post post = new Post(
                new Category("Design", "design", null),
                slug,
                slug + " title",
                slug + " description",
                "https://cdn.example.com/" + slug + ".jpg",
                slug + " alt");
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private Post publishedPost(Long id, String slug) {
        Post post = draftPost(id, slug);
        post.publish(Instant.parse("2026-09-01T00:00:00Z"));
        return post;
    }

    private Post archivedPost(Long id, String slug) {
        Post post = publishedPost(id, slug);
        post.archive();
        return post;
    }
}
