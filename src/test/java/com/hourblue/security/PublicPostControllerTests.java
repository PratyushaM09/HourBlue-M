package com.hourblue.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.hourblue.category.Category;
import com.hourblue.post.Post;
import com.hourblue.post.PostRepository;
import com.hourblue.post.PostStatus;
import com.hourblue.publicsite.PublicPostController;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PublicPostController.class)
@Import(SecurityConfiguration.class)
class PublicPostControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostRepository postRepository;

    @Test
    void homepageIsPublicAndRendersPublishedCards() throws Exception {
        Post post = publishedPost("published-post", "Published title", "https://cdn.example.com/post.jpg");
        when(postRepository.findAllByStatusOrderByPublishedAtDesc(eq(PostStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post)));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("HourBlue")))
                .andExpect(content().string(containsString("Published title")))
                .andExpect(content().string(containsString("https://cdn.example.com/post.jpg")))
                .andExpect(content().string(containsString("alt=\"Published title image\"")))
                .andExpect(content().string(containsString("Design")))
                .andExpect(content().string(containsString("/posts/published-post")))
                .andExpect(content().string(not(containsString("Draft title"))));
    }

    @Test
    void homepageShowsEmptyState() throws Exception {
        when(postRepository.findAllByStatusOrderByPublishedAtDesc(eq(PostStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No published posts yet.")));
    }

    @Test
    void homepageUsesFixedPaginationAndIgnoresClientSort() throws Exception {
        when(postRepository.findAllByStatusOrderByPublishedAtDesc(eq(PostStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/")
                        .param("page", "-2")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findAllByStatusOrderByPublishedAtDesc(eq(PostStatus.PUBLISHED), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(24, pageable.getValue().getPageSize());
        assertFalse(pageable.getValue().getSort().isSorted());
    }

    @Test
    void homepagePaginationLinksAreRendered() throws Exception {
        Post post = publishedPost("page-post", "Page title", "https://cdn.example.com/page.jpg");
        when(postRepository.findAllByStatusOrderByPublishedAtDesc(eq(PostStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post), PageRequest.of(1, 24), 50));

        mockMvc.perform(get("/").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/?page=0")))
                .andExpect(content().string(containsString("/?page=2")));
    }

    @Test
    void publishedPostDetailIsPublicAndRendersFields() throws Exception {
        Post post = publishedPost("published-post", "Published title", "https://cdn.example.com/post.jpg");
        when(postRepository.findBySlugAndStatus("published-post", PostStatus.PUBLISHED)).thenReturn(Optional.of(post));

        mockMvc.perform(get("/posts/published-post"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Published title - HourBlue")))
                .andExpect(content().string(containsString("Published title description")))
                .andExpect(content().string(containsString("https://cdn.example.com/post.jpg")))
                .andExpect(content().string(containsString("alt=\"Published title image\"")))
                .andExpect(content().string(containsString("Design")))
                .andExpect(content().string(containsString("2026-01-02T00:00:00Z")))
                .andExpect(content().string(containsString("href=\"https://example.com/source\"")))
                .andExpect(content().string(containsString("target=\"_blank\"")))
                .andExpect(content().string(containsString("rel=\"noopener noreferrer\"")));

        verify(postRepository).findBySlugAndStatus("published-post", PostStatus.PUBLISHED);
    }

    @Test
    void postDetailDoesNotRenderUnsafeSourceLinks() throws Exception {
        Post post = publishedPost("unsafe-source", "Unsafe source", "https://cdn.example.com/unsafe.jpg");
        when(postRepository.findBySlugAndStatus("unsafe-source", PostStatus.PUBLISHED)).thenReturn(Optional.of(post));

        mockMvc.perform(get("/posts/unsafe-source"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("href=\"javascript:alert(1)\""))))
                .andExpect(content().string(not(containsString("target=\"_blank\""))));
    }

    @Test
    void missingAndUnpublishedSlugsReturnNotFound() throws Exception {
        when(postRepository.findBySlugAndStatus("missing", PostStatus.PUBLISHED)).thenReturn(Optional.empty());
        when(postRepository.findBySlugAndStatus("draft", PostStatus.PUBLISHED)).thenReturn(Optional.empty());
        when(postRepository.findBySlugAndStatus("archived", PostStatus.PUBLISHED)).thenReturn(Optional.empty());

        mockMvc.perform(get("/posts/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Page not found.")))
                .andExpect(content().string(not(containsString("draft"))))
                .andExpect(content().string(not(containsString("archived"))));

        mockMvc.perform(get("/posts/draft"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/posts/archived"))
                .andExpect(status().isNotFound());
    }

    private Post publishedPost(String slug, String title, String imageUrl) {
        Post post = new Post(
                new Category("Design", "design", null),
                slug,
                title,
                title + " description",
                imageUrl,
                "images/" + slug,
                title + " image",
                sourceUrl(slug));
        post.publish(Instant.parse("2026-01-02T00:00:00Z"));
        return post;
    }

    private String sourceUrl(String slug) {
        if ("unsafe-source".equals(slug)) {
            return "javascript:alert(1)";
        }
        return "https://example.com/source";
    }
}
