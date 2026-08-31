package com.hourblue.publicsite;

import java.net.URI;

import com.hourblue.post.Post;
import com.hourblue.post.PostRepository;
import com.hourblue.post.PostStatus;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PublicPostController {

    private static final int PAGE_SIZE = 24;

    private final PostRepository postRepository;

    public PublicPostController(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @GetMapping("/")
    String index(@RequestParam(defaultValue = "0") int page, Model model) {
        int pageNumber = Math.max(page, 0);
        model.addAttribute("posts", postRepository.findAllByStatusOrderByPublishedAtDesc(
                PostStatus.PUBLISHED,
                PageRequest.of(pageNumber, PAGE_SIZE)));
        return "public/index";
    }

    @GetMapping("/posts/{slug}")
    String post(@PathVariable String slug, Model model, HttpServletResponse response) {
        return postRepository.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                .map(post -> postView(post, model))
                .orElseGet(() -> notFound(response));
    }

    private String postView(Post post, Model model) {
        model.addAttribute("post", post);
        model.addAttribute("metaDescription", metaDescription(post.getDescription()));
        model.addAttribute("sourceUrlSafe", isHttpUrl(post.getSourceUrl()));
        return "public/post";
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
