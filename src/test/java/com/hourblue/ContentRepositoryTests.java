package com.hourblue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import com.hourblue.category.Category;
import com.hourblue.category.CategoryRepository;
import com.hourblue.post.Post;
import com.hourblue.post.PostRepository;
import com.hourblue.post.PostStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ContentRepositoryTests {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Test
    void categoryCanBeSavedAndFoundBySlug() {
        Category category = categoryRepository.save(category("Category"));

        assertEquals(category.getId(), categoryRepository.findBySlug(category.getSlug()).orElseThrow().getId());
        assertTrue(categoryRepository.findBySlug("missing-" + unique()).isEmpty());
    }

    @Test
    void publishedPostCanBeFoundBySlugWithoutExposingDraftsOrArchivedPosts() {
        Category category = categoryRepository.save(category("Lookup"));
        Post published = post(category, "published");
        published.publish(Instant.parse("2026-01-02T00:00:00Z"));
        Post draft = post(category, "draft");
        Post archived = post(category, "archived");
        Instant archivedAt = Instant.parse("2026-01-03T00:00:00Z");
        archived.publish(archivedAt);
        archived.archive();

        assertEquals(archivedAt, archived.getPublishedAt());
        postRepository.save(published);
        postRepository.save(draft);
        postRepository.save(archived);

        assertEquals(
                published.getSlug(),
                postRepository.findBySlugAndStatus(published.getSlug(), PostStatus.PUBLISHED).orElseThrow().getSlug());
        assertTrue(postRepository.findBySlugAndStatus(draft.getSlug(), PostStatus.PUBLISHED).isEmpty());
        assertTrue(postRepository.findBySlugAndStatus(archived.getSlug(), PostStatus.PUBLISHED).isEmpty());
    }

    @Test
    void publishedListingExcludesOtherStatusesOrdersNewestFirstAndPaginates() {
        Category category = categoryRepository.save(category("Listing"));
        Post newest = post(category, "newest");
        newest.publish(Instant.parse("2099-01-03T00:00:00Z"));
        Post middle = post(category, "middle");
        middle.publish(Instant.parse("2099-01-02T00:00:00Z"));
        Post oldest = post(category, "oldest");
        oldest.publish(Instant.parse("2099-01-01T00:00:00Z"));
        Post draft = post(category, "listing-draft");
        Post archived = post(category, "listing-archived");
        archived.publish(Instant.parse("2099-01-04T00:00:00Z"));
        archived.archive();
        postRepository.save(newest);
        postRepository.save(middle);
        postRepository.save(oldest);
        postRepository.save(draft);
        postRepository.save(archived);

        Page<Post> page = postRepository.findAllByStatusOrderByPublishedAtDesc(
                PostStatus.PUBLISHED,
                PageRequest.of(0, 2));

        assertTrue(page.getTotalElements() >= 3);
        assertEquals(2, page.getContent().size());
        assertEquals(newest.getSlug(), page.getContent().get(0).getSlug());
        assertEquals(middle.getSlug(), page.getContent().get(1).getSlug());
    }

    @Test
    void categoryFilteringReturnsOnlyPublishedPostsInRequestedCategory() {
        Category requested = categoryRepository.save(category("Requested"));
        Category other = categoryRepository.save(category("Other"));
        Post requestedPublished = post(requested, "requested-published");
        requestedPublished.publish(Instant.parse("2026-01-02T00:00:00Z"));
        Post requestedDraft = post(requested, "requested-draft");
        Post otherPublished = post(other, "other-published");
        otherPublished.publish(Instant.parse("2026-01-03T00:00:00Z"));
        postRepository.save(requestedPublished);
        postRepository.save(requestedDraft);
        postRepository.save(otherPublished);

        Page<Post> page = postRepository.findAllByCategorySlugAndStatusOrderByPublishedAtDesc(
                requested.getSlug(),
                PostStatus.PUBLISHED,
                PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals(requestedPublished.getSlug(), page.getContent().get(0).getSlug());
    }

    @Test
    void imageReplacementPersistsImageFieldsOnly() {
        Category category = categoryRepository.save(category("Replacement"));
        Instant publishedAt = Instant.parse("2026-01-02T00:00:00Z");
        Post post = new Post(
                category,
                "replacement-" + unique(),
                "Replacement title",
                "Replacement description",
                "https://cdn.example.com/old.jpg",
                "public-old",
                "Replacement alt",
                "https://example.com/source");
        post.publish(publishedAt);
        postRepository.saveAndFlush(post);

        post.replaceImage("https://cdn.example.com/new.jpg", "public-new");
        postRepository.saveAndFlush(post);

        Post found = postRepository.findById(post.getId()).orElseThrow();
        assertEquals("https://cdn.example.com/new.jpg", found.getImageUrl());
        assertEquals("public-new", found.getCloudinaryPublicId());
        assertEquals(post.getSlug(), found.getSlug());
        assertEquals("Replacement title", found.getTitle());
        assertEquals("Replacement description", found.getDescription());
        assertEquals("Replacement alt", found.getAltText());
        assertEquals("https://example.com/source", found.getSourceUrl());
        assertEquals(PostStatus.PUBLISHED, found.getStatus());
        assertEquals(publishedAt, found.getPublishedAt());
    }

    private Category category(String label) {
        String unique = unique();
        return new Category(label + " " + unique, label.toLowerCase() + "-" + unique, null);
    }

    private Post post(Category category, String label) {
        String unique = unique();
        return new Post(
                category,
                label + "-" + unique,
                label + " title",
                label + " description",
                "https://example.com/" + unique + ".jpg",
                label + " alt text");
    }

    private String unique() {
        return UUID.randomUUID().toString();
    }
}
