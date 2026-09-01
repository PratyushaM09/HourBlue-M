package com.hourblue.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.hourblue.admin.post.AdminPostService;
import com.hourblue.admin.post.DuplicateSlugException;
import com.hourblue.admin.post.InvalidPostFormException;
import com.hourblue.admin.post.PostNotFoundException;
import com.hourblue.admin.post.PostForm;
import com.hourblue.category.Category;
import com.hourblue.category.CategoryRepository;
import com.hourblue.image.ImageStorageException;
import com.hourblue.post.Mood;
import com.hourblue.post.Post;
import com.hourblue.post.PostRepository;
import com.hourblue.subscriber.SubscriptionService;
import com.hourblue.today.TodayMomentService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest
@Import(SecurityConfiguration.class)
class AdminManagementControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private PostRepository postRepository;

    @MockitoBean
    private AdminPostService adminPostService;

    @MockitoBean
    private TodayMomentService todayMomentService;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @Test
    void unauthenticatedAdminManagementRequestsRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/login"));

        mockMvc.perform(get("/admin/posts"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/login"));

        mockMvc.perform(get("/admin/posts/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/login"));

        mockMvc.perform(get("/admin/today"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/login"));
    }

    @Test
    void adminHomeLinksToManagementPages() throws Exception {
        mockMvc.perform(get("/admin").with(user("admin@example.test")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/admin/categories")))
                .andExpect(content().string(containsString("/admin/posts")))
                .andExpect(content().string(containsString("/admin/today")));
    }

    @Test
    void authenticatedCategoryPageShowsCategoriesAndForm() throws Exception {
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(category(1L, "Design", "design")));

        mockMvc.perform(get("/admin/categories").with(user("admin@example.test")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Design")))
                .andExpect(content().string(containsString("name=\"name\"")))
                .andExpect(content().string(containsString("name=\"slug\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void categoryCreationWithCsrfNormalizesAndSaves() throws Exception {
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        mockMvc.perform(post("/admin/categories")
                        .with(user("admin@example.test"))
                        .with(csrf())
                        .param("name", "  Design  ")
                        .param("slug", "  Visual-Design  ")
                        .param("description", "  Ideas  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"));

        ArgumentCaptor<Category> category = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(category.capture());
        assertEquals("Design", category.getValue().getName());
        assertEquals("visual-design", category.getValue().getSlug());
        assertEquals("Ideas", category.getValue().getDescription());
    }

    @Test
    void categoryCreationRejectedWithoutCsrf() throws Exception {
        mockMvc.perform(post("/admin/categories")
                        .with(user("admin@example.test"))
                        .param("name", "Design")
                        .param("slug", "design"))
                .andExpect(status().isForbidden());

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void duplicateAndInvalidCategorySubmissionsReturnSafeFormErrors() throws Exception {
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of());
        when(categoryRepository.existsByName("Design")).thenReturn(true);
        when(categoryRepository.existsBySlug("design")).thenReturn(true);

        mockMvc.perform(post("/admin/categories")
                        .with(user("admin@example.test"))
                        .with(csrf())
                        .param("name", "Design")
                        .param("slug", "design"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Category name already exists.")))
                .andExpect(content().string(containsString("Category slug already exists.")))
                .andExpect(content().string(not(containsString("Duplicate entry"))));

        mockMvc.perform(post("/admin/categories")
                        .with(user("admin@example.test"))
                        .with(csrf())
                        .param("name", " ")
                        .param("slug", "-bad-")
                        .param("description", "A".repeat(256)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Name is required.")))
                .andExpect(content().string(containsString("Slug must be lowercase")))
                .andExpect(content().string(containsString("Description must be 255 characters or fewer.")));
    }

    @Test
    void postListUsesFixedPaginationAndNewFormShowsCategoryChoices() throws Exception {
        Category category = category(1L, "Design", "design");
        when(postRepository.findAllByOrderByCreatedAtDescIdDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(existingPost(5L, category))));
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(category));

        mockMvc.perform(get("/admin/posts")
                        .with(user("admin@example.test"))
                        .param("page", "-4")
                        .param("sort", "title"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Draft title")))
                .andExpect(content().string(containsString("Design")))
                .andExpect(content().string(containsString("/admin/posts/5/edit")));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findAllByOrderByCreatedAtDescIdDesc(pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(20, pageable.getValue().getPageSize());
        assertEquals(false, pageable.getValue().getSort().isSorted());

        mockMvc.perform(get("/admin/posts/new").with(user("admin@example.test")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("New post")))
                .andExpect(content().string(containsString("Design")))
                .andExpect(content().string(containsString("enctype=\"multipart/form-data\"")))
                .andExpect(content().string(containsString("name=\"mood\"")))
                .andExpect(content().string(containsString("Calm")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void editFormPopulatesPostMetadata() throws Exception {
        Category category = category(1L, "Design", "design");
        when(postRepository.findWithCategoryById(5L)).thenReturn(Optional.of(existingPost(5L, category)));
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(category));

        mockMvc.perform(get("/admin/posts/5/edit").with(user("admin@example.test")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edit post")))
                .andExpect(content().string(containsString("value=\"draft-post\"")))
                .andExpect(content().string(containsString("https://cdn.example.com/image.jpg")))
                .andExpect(content().string(containsString("Dreamy")))
                .andExpect(content().string(containsString("/admin/posts/5/image")))
                .andExpect(content().string(containsString("id=\"replacementImageFile\"")));
    }

    @Test
    void missingPostReturnsSafeNotFoundPage() throws Exception {
        when(postRepository.findWithCategoryById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/posts/404/edit").with(user("admin@example.test")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Post not found.")))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void createDraftDelegatesAndSafeErrorsPreserveForm() throws Exception {
        Category category = category(1L, "Design", "design");
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(category));
        MockMultipartFile image = new MockMultipartFile("imageFile", "post.jpg", "image/jpeg", new byte[] {1});

        mockMvc.perform(multipart("/admin/posts")
                        .file(image)
                        .with(user("admin@example.test"))
                        .with(csrf())
                        .param("categoryId", "1")
                        .param("title", "Draft title")
                        .param("slug", "draft-post")
                        .param("description", "Draft description")
                        .param("altText", "Draft image")
                        .param("sourceUrl", "https://example.com/source")
                        .param("mood", "CALM"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/posts"));

        ArgumentCaptor<PostForm> form = ArgumentCaptor.forClass(PostForm.class);
        verify(adminPostService).createDraft(form.capture(), any(MultipartFile.class));
        assertEquals("draft-post", form.getValue().getSlug());
        assertEquals(Mood.CALM, form.getValue().getMood());

        doThrow(new ImageStorageException()).when(adminPostService).createDraft(any(), any());
        mockMvc.perform(multipart("/admin/posts")
                        .file(image)
                        .with(user("admin@example.test"))
                        .with(csrf())
                        .param("categoryId", "1")
                        .param("title", "Draft title")
                        .param("slug", "draft-post")
                        .param("description", "Draft description")
                        .param("altText", "Draft image"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Image storage operation failed.")))
                .andExpect(content().string(containsString("value=\"Draft title\"")))
                .andExpect(content().string(containsString("Design")))
                .andExpect(content().string(not(containsString("cloudinary"))));

        doThrow(new InvalidPostFormException("Title is required.")).when(adminPostService).createDraft(any(), any());
        mockMvc.perform(multipart("/admin/posts")
                        .file(image)
                        .with(user("admin@example.test"))
                        .with(csrf())
                        .param("categoryId", "1")
                        .param("title", " ")
                        .param("slug", "draft-post")
                        .param("description", "Draft description")
                        .param("altText", "Draft image"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Title is required.")));
    }

    @Test
    void editDelegatesAndHandlesDuplicateSlugSafely() throws Exception {
        Category category = category(1L, "Design", "design");
        when(postRepository.findWithCategoryById(5L)).thenReturn(Optional.of(existingPost(5L, category)));
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(category));

        mockMvc.perform(post("/admin/posts/5")
                        .with(user("admin@example.test"))
                        .with(csrf())
                        .param("categoryId", "1")
                        .param("title", "Updated title")
                        .param("slug", "updated-post")
                        .param("description", "Updated description")
                        .param("altText", "Updated image")
                        .param("mood", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/posts"));

        ArgumentCaptor<PostForm> form = ArgumentCaptor.forClass(PostForm.class);
        verify(adminPostService).updateMetadata(org.mockito.Mockito.eq(5L), form.capture());
        assertEquals("updated-post", form.getValue().getSlug());
        assertNull(form.getValue().getMood());

        when(adminPostService.updateMetadata(org.mockito.Mockito.eq(5L), any())).thenThrow(new DuplicateSlugException());
        mockMvc.perform(post("/admin/posts/5")
                        .with(user("admin@example.test"))
                        .with(csrf())
                        .param("categoryId", "1")
                        .param("title", "Updated title")
                        .param("slug", "taken-post")
                        .param("description", "Updated description")
                        .param("altText", "Updated image"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Post slug already exists.")))
                .andExpect(content().string(containsString("value=\"taken-post\"")))
                .andExpect(content().string(not(containsString("Duplicate entry"))));
    }

    @Test
    void replaceImageRouteRequiresCsrfAndDelegates() throws Exception {
        MockMultipartFile image = new MockMultipartFile("imageFile", "post.jpg", "image/jpeg", new byte[] {1});

        mockMvc.perform(multipart("/admin/posts/5/image")
                        .file(image)
                        .with(user("admin@example.test")))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/admin/posts/5/image")
                        .file(image)
                        .with(user("admin@example.test"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/posts/5/edit"));

        verify(adminPostService).replaceImage(org.mockito.Mockito.eq(5L), any(MultipartFile.class));
    }

    @Test
    void replaceImageErrorsRemainSafe() throws Exception {
        MockMultipartFile image = new MockMultipartFile("imageFile", "post.jpg", "image/jpeg", new byte[] {1});

        doThrow(new ImageStorageException()).when(adminPostService)
                .replaceImage(org.mockito.Mockito.eq(5L), any());
        mockMvc.perform(multipart("/admin/posts/5/image")
                        .file(image)
                        .with(user("admin@example.test"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/posts/5/edit"))
                .andExpect(flash().attribute("errorMessage", "Image storage operation failed."));

        doThrow(new PostNotFoundException()).when(adminPostService)
                .replaceImage(org.mockito.Mockito.eq(404L), any());
        mockMvc.perform(multipart("/admin/posts/404/image")
                        .file(image)
                        .with(user("admin@example.test"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Post not found.")))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void publishAndArchiveRoutesRequireCsrfAndDelegate() throws Exception {
        mockMvc.perform(post("/admin/posts/5/publish").with(user("admin@example.test")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/posts/5/publish").with(user("admin@example.test")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/posts"));

        mockMvc.perform(post("/admin/posts/5/archive").with(user("admin@example.test")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/posts"));

        verify(adminPostService).publish(5L);
        verify(adminPostService).archive(5L);
    }

    @Test
    void invalidLifecycleTransitionRedirectsWithSafeError() throws Exception {
        doThrow(new IllegalStateException("Only draft posts can be published."))
                .when(adminPostService).publish(5L);

        mockMvc.perform(post("/admin/posts/5/publish").with(user("admin@example.test")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/posts"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "The post's current status does not allow this action."));
    }

    private Category category(Long id, String name, String slug) {
        Category category = new Category(name, slug, null);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private Post existingPost(Long id, Category category) {
        Post post = new Post(
                category,
                "draft-post",
                "Draft title",
                "Draft description",
                "https://cdn.example.com/image.jpg",
                "images/draft-post",
                "Draft image",
                null,
                Mood.DREAMY);
        ReflectionTestUtils.setField(post, "id", id);
        ReflectionTestUtils.setField(post, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        return post;
    }
}
