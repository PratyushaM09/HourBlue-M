package com.hourblue.admin.post;

import java.time.Instant;
import java.util.Set;

import com.hourblue.category.Category;
import com.hourblue.category.CategoryRepository;
import com.hourblue.image.CloudinaryImageStorage;
import com.hourblue.image.ImageStorageException;
import com.hourblue.image.UploadedImage;
import com.hourblue.post.Post;
import com.hourblue.post.PostRepository;
import com.hourblue.post.PostStatus;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminPostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectProvider<CloudinaryImageStorage> storageProvider;
    private final TransactionTemplate transactionTemplate;
    private final Validator validator;

    public AdminPostService(
            PostRepository postRepository,
            CategoryRepository categoryRepository,
            ObjectProvider<CloudinaryImageStorage> storageProvider,
            PlatformTransactionManager transactionManager,
            Validator validator) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
        this.storageProvider = storageProvider;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.validator = validator;
    }

    public Post createDraft(PostForm form, MultipartFile imageFile) {
        PostForm normalized = validateForm(form);
        Category category = categoryRepository.findById(normalized.getCategoryId())
                .orElseThrow(CategoryNotFoundException::new);
        ensureSlugAvailable(normalized.getSlug());

        CloudinaryImageStorage storage = storageProvider.getIfAvailable();
        if (storage == null) {
            throw new ImageStorageException();
        }

        UploadedImage uploaded = storage.upload(imageFile);
        Post draft = new Post(
                category,
                normalized.getSlug(),
                normalized.getTitle(),
                normalized.getDescription(),
                uploaded.secureUrl(),
                uploaded.publicId(),
                normalized.getAltText(),
                normalized.getSourceUrl());

        try {
            return transactionTemplate.execute(status -> postRepository.save(draft));
        } catch (RuntimeException persistenceFailure) {
            try {
                storage.delete(uploaded.publicId());
            } catch (RuntimeException cleanupFailure) {
                persistenceFailure.addSuppressed(cleanupFailure);
            }
            throw persistenceFailure;
        }
    }

    public Post updateMetadata(Long postId, PostForm form) {
        PostForm normalized = validateForm(form);

        return transactionTemplate.execute(status -> {
            Post post = postRepository.findById(postId)
                    .orElseThrow(PostNotFoundException::new);

            Category category = categoryRepository.findById(normalized.getCategoryId())
                    .orElseThrow(CategoryNotFoundException::new);

            if (!post.getSlug().equals(normalized.getSlug()) && post.getStatus() != PostStatus.DRAFT) {
                throw new IllegalStateException("Only draft posts can change slug.");
            }

            if (!post.getSlug().equals(normalized.getSlug())) {
                ensureSlugAvailable(normalized.getSlug());
            }

            post.updateMetadata(
                    category,
                    normalized.getTitle(),
                    normalized.getSlug(),
                    normalized.getDescription(),
                    normalized.getAltText(),
                    normalized.getSourceUrl());

            return post;
        });
    }

    public Post publish(Long postId) {
        return transactionTemplate.execute(status -> {
            Post post = postRepository.findById(postId)
                    .orElseThrow(PostNotFoundException::new);
            post.publish(Instant.now());
            return post;
        });
    }

    public Post archive(Long postId) {
        return transactionTemplate.execute(status -> {
            Post post = postRepository.findById(postId)
                    .orElseThrow(PostNotFoundException::new);
            post.archive();
            return post;
        });
    }

    private PostForm validateForm(PostForm form) {
        if (form == null) {
            throw new InvalidPostFormException("Post form is required.");
        }

        form.normalize();

        Set<ConstraintViolation<PostForm>> violations = validator.validate(form);
        if (!violations.isEmpty()) {
            throw new InvalidPostFormException(violations.iterator().next().getMessage());
        }

        return form;
    }

    private void ensureSlugAvailable(String slug) {
        if (postRepository.findBySlug(slug).isPresent()) {
            throw new DuplicateSlugException();
        }
    }
}
