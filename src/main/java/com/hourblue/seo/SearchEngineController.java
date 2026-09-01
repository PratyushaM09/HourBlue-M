package com.hourblue.seo;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.hourblue.category.CategoryRepository;
import com.hourblue.post.Mood;
import com.hourblue.post.PostRepository;
import com.hourblue.post.PostStatus;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@RestController
public class SearchEngineController {

    private static final String SITEMAP_NAMESPACE = "http://www.sitemaps.org/schemas/sitemap/0.9";

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final SiteUrlBuilder siteUrlBuilder;

    public SearchEngineController(
            CategoryRepository categoryRepository,
            PostRepository postRepository,
            SiteUrlBuilder siteUrlBuilder) {
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
        this.siteUrlBuilder = siteUrlBuilder;
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    String robots() {
        return """
                User-agent: *
                Allow: /
                Disallow: /admin/
                Sitemap: %s
                """.formatted(siteUrlBuilder.canonical("/sitemap.xml"));
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    String sitemap() throws ParserConfigurationException, TransformerException {
        List<String> locations = new ArrayList<>();
        locations.add(siteUrlBuilder.canonical("/"));
        categoryRepository.findAllProjectedByOrderBySlugAsc()
                .forEach(category -> locations.add(siteUrlBuilder.canonical("/categories/" + category.getSlug())));
        for (Mood mood : Mood.values()) {
            locations.add(siteUrlBuilder.canonical("/moods/" + mood.getSlug()));
        }
        postRepository.findAllProjectedByStatusOrderBySlugAsc(PostStatus.PUBLISHED)
                .forEach(post -> locations.add(siteUrlBuilder.canonical("/posts/" + post.getSlug())));

        return sitemapXml(locations);
    }

    private String sitemapXml(List<String> locations) throws ParserConfigurationException, TransformerException {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element urlset = document.createElementNS(SITEMAP_NAMESPACE, "urlset");
        document.appendChild(urlset);

        for (String location : locations) {
            Element url = document.createElementNS(SITEMAP_NAMESPACE, "url");
            Element loc = document.createElementNS(SITEMAP_NAMESPACE, "loc");
            loc.setTextContent(location);
            url.appendChild(loc);
            urlset.appendChild(url);
        }

        StringWriter writer = new StringWriter();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }
}
