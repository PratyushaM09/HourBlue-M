package com.hourblue.publicsite;

import java.net.URI;

import com.hourblue.category.Category;
import com.hourblue.category.CategoryRepository;
import com.hourblue.post.Mood;
import com.hourblue.post.Post;
import com.hourblue.post.PostRepository;
import com.hourblue.post.PostStatus;
import com.hourblue.today.TodayMomentService;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PublicPostController {

    private static final int PAGE_SIZE = 24;

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final TodayMomentService todayMomentService;

    public PublicPostController(
            CategoryRepository categoryRepository,
            PostRepository postRepository,
            TodayMomentService todayMomentService) {
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
        this.todayMomentService = todayMomentService;
    }

    @GetMapping("/")
    String index(@RequestParam(defaultValue = "0") int page, Model model) {
        int pageNumber = Math.max(page, 0);
        model.addAttribute("posts", postRepository.findAllByStatusOrderByPublishedAtDesc(
                PostStatus.PUBLISHED,
                PageRequest.of(pageNumber, PAGE_SIZE)));
        model.addAttribute("todayMoment", todayMomentService.resolveTodayMoment().orElse(null));
        return "public/index";
    }

    @GetMapping("/posts/{slug}")
    String post(@PathVariable String slug, Model model, HttpServletResponse response) {
        return postRepository.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                .map(post -> postView(post, model))
                .orElseGet(() -> notFound(response));
    }

    @GetMapping("/categories/{slug}")
    String category(@PathVariable String slug, @RequestParam(defaultValue = "0") int page, Model model,
            HttpServletResponse response) {
        return categoryRepository.findBySlug(slug)
                .map(category -> browseCategory(category, page, model))
                .orElseGet(() -> notFound(response));
    }

    @GetMapping("/moods/{slug}")
    String mood(@PathVariable String slug, @RequestParam(defaultValue = "0") int page, Model model,
            HttpServletResponse response) {
        return Mood.fromSlug(slug)
                .map(mood -> browseMood(mood, page, model))
                .orElseGet(() -> notFound(response));
    }

    private String postView(Post post, Model model) {
        model.addAttribute("post", post);
        model.addAttribute("metaDescription", metaDescription(post.getDescription()));
        model.addAttribute("sourceUrlSafe", isHttpUrl(post.getSourceUrl()));
        return "public/post";
    }

    private String browseCategory(Category category, int page, Model model) {
        Page<Post> posts = postRepository.findAllByCategoryAndStatusOrderByPublishedAtDesc(
                category,
                PostStatus.PUBLISHED,
                pageRequest(page));
        return browse(model, posts, "Category", category.getName(), "/categories/" + category.getSlug());
    }

    private String browseMood(Mood mood, int page, Model model) {
        Page<Post> posts = postRepository.findAllByMoodAndStatusOrderByPublishedAtDesc(
                mood,
                PostStatus.PUBLISHED,
                pageRequest(page));
        return browse(model, posts, "Mood", mood.getDisplayName(), "/moods/" + mood.getSlug());
    }

    private String browse(Model model, Page<Post> posts, String browseType, String browseName, String path) {
        model.addAttribute("posts", posts);
        model.addAttribute("browseType", browseType);
        model.addAttribute("browseName", browseName);
        model.addAttribute("path", path);
        model.addAttribute("previousPageUrl", posts.hasPrevious() ? path + "?page=" + (posts.getNumber() - 1) : null);
        model.addAttribute("nextPageUrl", posts.hasNext() ? path + "?page=" + (posts.getNumber() + 1) : null);
        return "public/browse";
    }

    private PageRequest pageRequest(int page) {
        return PageRequest.of(Math.max(page, 0), PAGE_SIZE);
    }

    private String notFound(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return "public/not-found";
    }

    private String metaDescription(String description) {
        if (description.length() <= 160) {
            return description;
        }
        return description.substring(0, 157) + "...";
    }

    private boolean isHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(value.strip());
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
