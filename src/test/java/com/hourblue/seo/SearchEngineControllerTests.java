package com.hourblue.seo;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import com.hourblue.category.CategoryRepository;
import com.hourblue.post.Mood;
import com.hourblue.post.PostRepository;
import com.hourblue.post.PostStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.xml.sax.InputSource;

@WebMvcTest(SearchEngineController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SiteUrlBuilder.class)
@TestPropertySource(properties = "hourblue.site-base-url=https://hourblue.example")
class SearchEngineControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private PostRepository postRepository;

    @Test
    void robotsTxtIsPublicAndReferencesSitemap() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("User-agent: *")))
                .andExpect(content().string(containsString("Allow: /")))
                .andExpect(content().string(containsString("Disallow: /admin/")))
                .andExpect(content().string(containsString("Sitemap: https://hourblue.example/sitemap.xml")))
                .andExpect(content().string(not(containsString("DB_PASSWORD"))));
    }

    @Test
    void robotsTxtIgnoresHostHeader() throws Exception {
        mockMvc.perform(get("/robots.txt").header("Host", "attacker.example"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sitemap: https://hourblue.example/sitemap.xml")))
                .andExpect(content().string(not(containsString("attacker.example"))));
    }

    @Test
    void sitemapXmlIncludesPublicUrlsOnly() throws Exception {
        CategoryRepository.SlugOnly category = () -> "design";
        PostRepository.SlugOnly post = () -> "published-post";
        when(categoryRepository.findAllProjectedByOrderBySlugAsc()).thenReturn(List.of(category));
        when(postRepository.findAllProjectedByStatusOrderBySlugAsc(PostStatus.PUBLISHED))
                .thenReturn(List.of(post));

        MvcResult result = mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("<loc>https://hourblue.example/</loc>")))
                .andExpect(content().string(containsString("<loc>https://hourblue.example/categories/design</loc>")))
                .andExpect(content().string(containsString("<loc>https://hourblue.example/posts/published-post</loc>")))
                .andExpect(content().string(containsString("<loc>https://hourblue.example/moods/" + Mood.CALM.getSlug() + "</loc>")))
                .andExpect(content().string(not(containsString("/admin/"))))
                .andExpect(content().string(not(containsString("/subscribe"))))
                .andReturn();

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        var document = documentBuilderFactory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(result.getResponse().getContentAsString())));
        assertEquals("urlset", document.getDocumentElement().getLocalName());
        assertEquals("http://www.sitemaps.org/schemas/sitemap/0.9",
                document.getDocumentElement().getNamespaceURI());
    }
}
