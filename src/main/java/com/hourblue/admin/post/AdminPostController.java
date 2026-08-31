package com.hourblue.admin.post;

import com.hourblue.category.CategoryRepository;
import com.hourblue.image.ImageStorageException;
import com.hourblue.post.Post;
import com.hourblue.post.PostRepository;
import com.hourblue.post.PostStatus;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminPostController {

    private static final int PAGE_SIZE = 20;

    private final AdminPostService adminPostService;
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;

    public AdminPostController(
            AdminPostService adminPostService,
            PostRepository postRepository,
            CategoryRepository categoryRepository) {
        this.adminPostService = adminPostService;
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/admin/posts")
    String index(@RequestParam(defaultValue = "0") int page, Model model) {
        int pageNumber = Math.max(page, 0);
        model.addAttribute("posts", postRepository.findAllByOrderByCreatedAtDescIdDesc(
                PageRequest.of(pageNumber, PAGE_SIZE)));
        return "admin/posts/index";
    }

    @GetMapping("/admin/posts/new")
    String newPost(Model model) {
        if (!model.containsAttribute("postForm")) {
            model.addAttribute("postForm", new PostForm());
        }
        addFormAttributes(model, "/admin/posts", true, true);
        return "admin/posts/form";
    }

    @PostMapping("/admin/posts")
    String create(
            @ModelAttribute("postForm") PostForm form,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            Model model) {
        try {
            adminPostService.createDraft(form, imageFile);
            return "redirect:/admin/posts";
        } catch (RuntimeException exception) {
            model.addAttribute("errorMessage", safePostError(exception));
            addFormAttributes(model, "/admin/posts", true, true);
            return "admin/posts/form";
        }
    }

    @GetMapping("/admin/posts/{id}/edit")
    String edit(@PathVariable Long id, Model model, HttpServletResponse response) {
        return postRepository.findWithCategoryById(id)
                .map(post -> {
                    model.addAttribute("postForm", formFrom(post));
                    addFormAttributes(model, "/admin/posts/" + id, post.getStatus() == PostStatus.DRAFT, false);
                    return "admin/posts/form";
                })
                .orElseGet(() -> notFound(model, response, "Post not found."));
    }

    @PostMapping("/admin/posts/{id}")
    String update(
            @PathVariable Long id,
            @ModelAttribute("postForm") PostForm form,
            Model model,
            HttpServletResponse response) {
        try {
            adminPostService.updateMetadata(id, form);
            return "redirect:/admin/posts";
        } catch (PostNotFoundException exception) {
            return notFound(model, response, exception.getMessage());
        } catch (RuntimeException exception) {
            Post post = postRepository.findWithCategoryById(id).orElse(null);
            if (post == null) {
                return notFound(model, response, "Post not found.");
            }
            model.addAttribute("errorMessage", safePostError(exception));
            addFormAttributes(model, "/admin/posts/" + id, post.getStatus() == PostStatus.DRAFT, false);
            return "admin/posts/form";
        }
    }

    @PostMapping("/admin/posts/{id}/publish")
    String publish(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            Model model,
            HttpServletResponse response) {
        return updateStatus(id, redirectAttributes, model, response, true);
    }

    @PostMapping("/admin/posts/{id}/archive")
    String archive(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            Model model,
            HttpServletResponse response) {
        return updateStatus(id, redirectAttributes, model, response, false);
    }

    private String updateStatus(
            Long id,
            RedirectAttributes redirectAttributes,
            Model model,
            HttpServletResponse response,
            boolean publish) {
        try {
            if (publish) {
                adminPostService.publish(id);
            } else {
                adminPostService.archive(id);
            }
            return "redirect:/admin/posts";
        } catch (PostNotFoundException exception) {
            return notFound(model, response, exception.getMessage());
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", safePostError(exception));
            return "redirect:/admin/posts";
        }
    }

    private void addFormAttributes(Model model, String action, boolean slugEditable, boolean imageRequired) {
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        model.addAttribute("action", action);
        model.addAttribute("slugEditable", slugEditable);
        model.addAttribute("imageRequired", imageRequired);
    }

    private PostForm formFrom(Post post) {
        PostForm form = new PostForm();
        form.setCategoryId(post.getCategory().getId());
        form.setTitle(post.getTitle());
        form.setSlug(post.getSlug());
        form.setDescription(post.getDescription());
        form.setAltText(post.getAltText());
        form.setSourceUrl(post.getSourceUrl());
        return form;
    }

    private String notFound(Model model, HttpServletResponse response, String message) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        model.addAttribute("message", message);
        return "admin/not-found";
    }

    private String safePostError(RuntimeException exception) {
        if (exception instanceof CategoryNotFoundException) {
            return "Selected category was not found.";
        }
        if (exception instanceof DuplicateSlugException
                || exception instanceof ImageStorageException
                || exception instanceof InvalidPostFormException) {
            return exception.getMessage();
        }
        if (exception instanceof IllegalStateException) {
            return "The post's current status does not allow this action.";
        }
        return "Post could not be saved.";
    }
}
