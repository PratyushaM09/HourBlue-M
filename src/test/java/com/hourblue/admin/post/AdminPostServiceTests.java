package com.hourblue.admin.post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.hourblue.category.Category;
import com.hourblue.category.CategoryRepository;
import com.hourblue.image.CloudinaryImageStorage;
import com.hourblue.image.ImageStorageException;
import com.hourblue.image.UploadedImage;
import com.hourblue.post.Post;
import com.hourblue.post.PostRepository;
import com.hourblue.post.PostStatus;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class AdminPostServiceTests {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ObjectProvider<CloudinaryImageStorage> storageProvider;

    @Mock
    private CloudinaryImageStorage cloudinaryImageStorage;

    @Test
    void successfulDraftUploadPersistsMappedPost() {
        Category category = new Category("Design", "design", "Design");
        PostForm form = new PostForm();
        form.setCategoryId(10L);
        form.setTitle("Hello world");
        form.setSlug("hello-world");
        form.setDescription("A thoughtful article.");
        form.setAltText("A preview image");
        form.setSourceUrl("https://example.com/article");
        MultipartFile file = mock(MultipartFile.class);
        UploadedImage uploaded = new UploadedImage("https://cdn.example.com/images/hello-world.jpg", "images/hello-world");

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(postRepository.findBySlug("hello-world")).thenReturn(Optional.empty());
        when(storageProvider.getIfAvailable()).thenReturn(cloudinaryImageStorage);
        when(cloudinaryImageStorage.upload(file)).thenReturn(uploaded);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminPostService service = new AdminPostService(postRepository, categoryRepository, storageProvider, mock(PlatformTransactionManager.class), VALIDATOR);
        Post saved = service.createDraft(form, file);

        assertNotNull(saved);
        assertEquals("hello-world", saved.getSlug());
        assertEquals("https://cdn.example.com/images/hello-world.jpg", saved.getImageUrl());
        assertEquals("images/hello-world", saved.getCloudinaryPublicId());
        assertEquals(PostStatus.DRAFT, saved.getStatus());
    }

    @Test
    void missingStorageStopsBeforePersistence() {
        Category category = new Category("Design", "design", "Design");
        PostForm form = new PostForm();
        form.setCategoryId(10L);
        form.setTitle("Hello world");
        form.setSlug("hello-world");
        form.setDescription("A thoughtful article.");
        form.setAltText("A preview image");
        form.setSourceUrl(null);
        MultipartFile file = mock(MultipartFile.class);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(postRepository.findBySlug("hello-world")).thenReturn(Optional.empty());
        when(storageProvider.getIfAvailable()).thenReturn(null);

        AdminPostService service = new AdminPostService(postRepository, categoryRepository, storageProvider, mock(PlatformTransactionManager.class), VALIDATOR);

        assertThrows(ImageStorageException.class, () -> service.createDraft(form, file));
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void missingCategoryPreventsUpload() {
        PostForm form = new PostForm();
        form.setCategoryId(10L);
        form.setTitle("Hello world");
        form.setSlug("hello-world");
        form.setDescription("A thoughtful article.");
        form.setAltText("A preview image");
        form.setSourceUrl(null);
        MultipartFile file = mock(MultipartFile.class);

        when(categoryRepository.findById(10L)).thenReturn(Optional.empty());

        AdminPostService service = new AdminPostService(postRepository, categoryRepository, storageProvider, mock(PlatformTransactionManager.class), VALIDATOR);

        assertThrows(CategoryNotFoundException.class, () -> service.createDraft(form, file));
    }

    @Test
    void duplicateSlugPreventsUpload() {
        Category category = new Category("Design", "design", "Design");
        PostForm form = new PostForm();
        form.setCategoryId(10L);
        form.setTitle("Hello world");
        form.setSlug("hello-world");
        form.setDescription("A thoughtful article.");
        form.setAltText("A preview image");
        form.setSourceUrl(null);
        MultipartFile file = mock(MultipartFile.class);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(postRepository.findBySlug("hello-world")).thenReturn(Optional.of(new Post(category, "hello-world", "Existing", "Body", "https://cdn.example.com/image.jpg", "existing-id", "Alt text", null)));

        AdminPostService service = new AdminPostService(postRepository, categoryRepository, storageProvider, mock(PlatformTransactionManager.class), VALIDATOR);

        assertThrows(DuplicateSlugException.class, () -> service.createDraft(form, file));
        verify(storageProvider, never()).getIfAvailable();
    }

    @Test
    void databaseFailureTriggersUploadCleanup() {
        Category category = new Category("Design", "design", "Design");
        PostForm form = new PostForm();
        form.setCategoryId(10L);
        form.setTitle("Hello world");
        form.setSlug("hello-world");
        form.setDescription("A thoughtful article.");
        form.setAltText("A preview image");
        form.setSourceUrl(null);
        MultipartFile file = mock(MultipartFile.class);
        UploadedImage uploaded = new UploadedImage("https://cdn.example.com/images/hello-world.jpg", "images/hello-world");

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(postRepository.findBySlug("hello-world")).thenReturn(Optional.empty());
        when(storageProvider.getIfAvailable()).thenReturn(cloudinaryImageStorage);
        when(cloudinaryImageStorage.upload(file)).thenReturn(uploaded);
        when(postRepository.save(any(Post.class))).thenThrow(new RuntimeException("db down"));

        AdminPostService service = new AdminPostService(postRepository, categoryRepository, storageProvider, transactionTemplate(), VALIDATOR);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createDraft(form, file));
        assertEquals("db down", ex.getMessage());
        verify(cloudinaryImageStorage).delete("images/hello-world");
    }

    @Test
    void cleanupFailureIsSuppressedWithoutReplacingOriginalError() {
        Category category = new Category("Design", "design", "Design");
        PostForm form = new PostForm();
        form.setCategoryId(10L);
        form.setTitle("Hello world");
        form.setSlug("hello-world");
        form.setDescription("A thoughtful article.");
        form.setAltText("A preview image");
        form.setSourceUrl(null);
        MultipartFile file = mock(MultipartFile.class);
        UploadedImage uploaded = new UploadedImage("https://cdn.example.com/images/hello-world.jpg", "images/hello-world");

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(postRepository.findBySlug("hello-world")).thenReturn(Optional.empty());
        when(storageProvider.getIfAvailable()).thenReturn(cloudinaryImageStorage);
        when(cloudinaryImageStorage.upload(file)).thenReturn(uploaded);
        when(postRepository.save(any(Post.class))).thenThrow(new RuntimeException("db down"));
        doThrow(new ImageStorageException()).when(cloudinaryImageStorage).delete("images/hello-world");

        AdminPostService service = new AdminPostService(postRepository, categoryRepository, storageProvider, transactionTemplate(), VALIDATOR);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createDraft(form, file));
        assertEquals("db down", ex.getMessage());
        assertTrue(ex.getSuppressed().length > 0);
        assertTrue(ex.getSuppressed()[0] instanceof ImageStorageException);
    }

    @Test
    void metadataEditDoesNotUploadOrDeleteImages() {
        Category category = new Category("Design", "design", "Design");
        Post post = new Post(category, "old-slug", "Old title", "Old description", "https://cdn.example.com/old.jpg", "public-old", "Old alt", null);
        PostForm form = new PostForm();
        form.setCategoryId(10L);
        form.setTitle("Updated title");
        form.setSlug("new-slug");
        form.setDescription("Updated description");
        form.setAltText("Updated alt");
        form.setSourceUrl("https://example.com/post");

        when(postRepository.findById(5L)).thenReturn(Optional.of(post));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(postRepository.findBySlug("new-slug")).thenReturn(Optional.empty());

        AdminPostService service = new AdminPostService(postRepository, categoryRepository, storageProvider, transactionTemplate(), VALIDATOR);
        Post updated = service.updateMetadata(5L, form);

        assertEquals("new-slug", updated.getSlug());
        assertEquals("https://cdn.example.com/old.jpg", updated.getImageUrl());
        assertEquals("public-old", updated.getCloudinaryPublicId());
        verify(cloudinaryImageStorage, never()).upload(any());
        verify(cloudinaryImageStorage, never()).delete(any());
    }

    @Test
    void publishAndArchiveDoNotCallCloudinary() {
        Category category = new Category("Design", "design", "Design");
        Post post = new Post(category, "draft-post", "Draft title", "Draft description", "https://cdn.example.com/image.jpg", "public-old", "Alt text", null);

        when(postRepository.findById(5L)).thenReturn(Optional.of(post));
        AdminPostService service = new AdminPostService(postRepository, categoryRepository, storageProvider, transactionTemplate(), VALIDATOR);

        service.publish(5L);
        service.archive(5L);

        verify(cloudinaryImageStorage, never()).upload(any());
        verify(cloudinaryImageStorage, never()).delete(any());
    }

    @Test
    void missingPostOrCategoryProducesSafeErrors() {
        PostForm form = new PostForm();
        form.setCategoryId(10L);
        form.setTitle("Updated title");
        form.setSlug("new-slug");
        form.setDescription("Updated description");
        form.setAltText("Updated alt");
        form.setSourceUrl(null);

        when(postRepository.findById(99L)).thenReturn(Optional.empty());
        AdminPostService service = new AdminPostService(postRepository, categoryRepository, storageProvider, transactionTemplate(), VALIDATOR);

        assertThrows(PostNotFoundException.class, () -> service.publish(99L));
        assertThrows(PostNotFoundException.class, () -> service.archive(99L));
        assertThrows(PostNotFoundException.class, () -> service.updateMetadata(99L, form));

        Category category = new Category("Design", "design", "Design");
        Post post = new Post(category, "old-slug", "Old title", "Old description", "https://cdn.example.com/image.jpg", "public-old", "Old alt", null);
        when(postRepository.findById(7L)).thenReturn(Optional.of(post));
        when(categoryRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(CategoryNotFoundException.class, () -> service.updateMetadata(7L, form));
    }

    private static PlatformTransactionManager transactionTemplate() {
        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
        TransactionStatus status = mock(TransactionStatus.class);
        when(txManager.getTransaction(any())).thenReturn(status);
        return txManager;
    }
}
