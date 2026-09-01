package com.hourblue.seo;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import com.hourblue.category.Category;
import com.hourblue.category.CategoryRepository;
import com.hourblue.post.Post;
import com.hourblue.post.PostRepository;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SitemapIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Test
    void robotsTxtIsAnonymousAndPublic() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("Allow: /")))
                .andExpect(content().string(containsString("Disallow: /admin/")))
                .andExpect(content().string(containsString("Sitemap: http://localhost:8080/sitemap.xml")));
    }

    @Test
    void sitemapFiltersActualPostsByPublishedStatus() throws Exception {
        Category category = categoryRepository.saveAndFlush(category());
        Post published = post(category, "published");
        published.publish(Instant.parse("2026-01-02T00:00:00Z"));
        Post draft = post(category, "draft");
        Post archived = post(category, "archived");
        archived.publish(Instant.parse("2026-01-03T00:00:00Z"));
        archived.archive();
        postRepository.saveAndFlush(published);
        postRepository.saveAndFlush(draft);
        postRepository.saveAndFlush(archived);

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/categories/" + category.getSlug())))
                .andExpect(content().string(containsString("/posts/" + published.getSlug())))
                .andExpect(content().string(not(containsString("/posts/" + draft.getSlug()))))
                .andExpect(content().string(not(containsString("/posts/" + archived.getSlug()))));
    }

    private Category category() {
        String unique = unique();
        return new Category("SEO " + unique, "seo-" + unique, null);
    }

    private Post post(Category category, String label) {
        String unique = unique();
        return new Post(
                category,
                label + "-" + unique,
                label + " title",
                label + " description",
                "https://cdn.example.com/" + unique + ".jpg",
                "images/" + unique,
                label + " image",
                null);
    }

    private String unique() {
        return UUID.randomUUID().toString();
    }
}
