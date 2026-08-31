package com.hourblue.today;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import com.hourblue.category.Category;
import com.hourblue.post.Post;

import org.junit.jupiter.api.Test;

class TodayMomentTests {

    @Test
    void assignmentStoresDateAndPost() {
        LocalDate date = LocalDate.of(2026, 9, 1);
        Post post = post("first");

        TodayMoment todayMoment = new TodayMoment(date, post);

        assertEquals(date, todayMoment.getFeatureDate());
        assertEquals(post, todayMoment.getPost());
    }

    @Test
    void replacingPostPreservesDate() {
        LocalDate date = LocalDate.of(2026, 9, 1);
        TodayMoment todayMoment = new TodayMoment(date, post("first"));
        Post replacement = post("replacement");

        todayMoment.replacePost(replacement);

        assertEquals(date, todayMoment.getFeatureDate());
        assertEquals(replacement, todayMoment.getPost());
    }

    private Post post(String slug) {
        return new Post(
                new Category("Design", "design", null),
                slug,
                slug + " title",
                slug + " description",
                "https://cdn.example.com/" + slug + ".jpg",
                slug + " alt");
    }
}
