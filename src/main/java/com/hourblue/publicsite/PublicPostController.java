package com.hourblue.publicsite;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hourblue.category.Category;
import com.hourblue.category.CategoryRepository;
import com.hourblue.post.Mood;
import com.hourblue.post.Post;
import com.hourblue.post.PostRepository;
import com.hourblue.post.PostStatus;
import com.hourblue.seo.SiteUrlBuilder;
import com.hourblue.subscriber.SubscriptionForm;
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
    private static final String HOMEPAGE_TITLE = "HourBlue - Visual Finds for Quiet Wandering";
    private static final String HOMEPAGE_DESCRIPTION =
            "HourBlue is a quiet visual-discovery gallery of curated published posts.";

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final TodayMomentService todayMomentService;
    private final SiteUrlBuilder siteUrlBuilder;
    private final ObjectMapper objectMapper;

    public PublicPostController(
            CategoryRepository categoryRepository,
            PostRepository postRepository,
            TodayMomentService todayMomentService,
            SiteUrlBuilder siteUrlBuilder,
            ObjectMapper objectMapper) {
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
        this.todayMomentService = todayMomentService;
        this.siteUrlBuilder = siteUrlBuilder;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    String index(@RequestParam(defaultValue = "0") int page, Model model) {
        int pageNumber = Math.max(page, 0);
        if (!model.containsAttribute("subscriptionForm")) {
            model.addAttribute("subscriptionForm", new SubscriptionForm());
        }
        model.addAttribute("posts", postRepository.findAllByStatusOrderByPublishedAtDesc(
                PostStatus.PUBLISHED,
                PageRequest.of(pageNumber, PAGE_SIZE)));
        model.addAttribute("todayMoment", todayMomentService.resolveTodayMoment().orElse(null));
        addSeo(model, HOMEPAGE_TITLE, HOMEPAGE_DESCRIPTION, siteUrlBuilder.canonical("/", pageNumber), "website");
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
        String description = postDescription(post);
        String canonicalUrl = siteUrlBuilder.canonical("/posts/" + post.getSlug());
        model.addAttribute("post", post);
        model.addAttribute("structuredDataJson", structuredData(post, description, canonicalUrl));
        model.addAttribute("sourceUrlSafe", isHttpUrl(post.getSourceUrl()));
        addSeo(model, post.getTitle() + " - HourBlue", description, canonicalUrl, "article",
                post.getImageUrl(), post.getAltText());
        return "public/post";
    }

    private String browseCategory(Category category, int page, Model model) {
        int pageNumber = Math.max(page, 0);
        Page<Post> posts = postRepository.findAllByCategoryAndStatusOrderByPublishedAtDesc(
                category,
                PostStatus.PUBLISHED,
                PageRequest.of(pageNumber, PAGE_SIZE));
        String path = "/categories/" + category.getSlug();
        addSeo(
                model,
                category.getName() + " - HourBlue",
                "Browse published HourBlue finds in " + category.getName() + ".",
                siteUrlBuilder.canonical(path, pageNumber),
                "website");
        return browse(model, posts, "Category", category.getName(), path);
    }

    private String browseMood(Mood mood, int page, Model model) {
        int pageNumber = Math.max(page, 0);
        Page<Post> posts = postRepository.findAllByMoodAndStatusOrderByPublishedAtDesc(
                mood,
                PostStatus.PUBLISHED,
                PageRequest.of(pageNumber, PAGE_SIZE));
        String path = "/moods/" + mood.getSlug();
        addSeo(
                model,
                mood.getDisplayName() + " - HourBlue",
                "Browse " + mood.getDisplayName().toLowerCase(Locale.ROOT) + " visual finds on HourBlue.",
                siteUrlBuilder.canonical(path, pageNumber),
                "website");
        return browse(model, posts, "Mood", mood.getDisplayName(), path);
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

    private String notFound(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return "public/not-found";
    }

    private void addSeo(
            Model model,
            String title,
            String description,
            String canonicalUrl,
            String ogType) {
        addSeo(model, title, description, canonicalUrl, ogType, null, null);
    }

    private void addSeo(
            Model model,
            String title,
            String description,
            String canonicalUrl,
            String ogType,
            String ogImageUrl,
            String ogImageAlt) {
        model.addAttribute("seoTitle", title);
        model.addAttribute("seoDescription", description);
        model.addAttribute("canonicalUrl", canonicalUrl);
        model.addAttribute("ogType", ogType);
        model.addAttribute("ogImageUrl", ogImageUrl);
        model.addAttribute("ogImageAlt", ogImageAlt);
    }

    private String postDescription(Post post) {
        String description = post.getDescription();
        if (description == null || description.isBlank()) {
            description = "Browse " + post.getTitle() + " in " + post.getCategory().getName() + " on HourBlue.";
        }
        return metaDescription(description);
    }

    private String metaDescription(String description) {
        String normalized = description.strip();
        if (normalized.length() <= 160) {
            return normalized;
        }
        return normalized.substring(0, 157) + "...";
    }

    private String structuredData(Post post, String description, String canonicalUrl) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("@context", "https://schema.org");
        data.put("@type", "ImageObject");
        data.put("name", post.getTitle());
        data.put("description", description);
        data.put("contentUrl", post.getImageUrl());
        data.put("url", canonicalUrl);
        try {
            return objectMapper.writeValueAsString(data)
                    .replace("&", "\\u0026")
                    .replace("<", "\\u003c")
                    .replace(">", "\\u003e");
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Post structured data could not be serialized.", exception);
        }
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
