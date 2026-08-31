package com.hourblue.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.hourblue.admin.today.AdminTodayMomentController;
import com.hourblue.category.Category;
import com.hourblue.post.Post;
import com.hourblue.today.InvalidTodayMomentException;
import com.hourblue.today.TodayMoment;
import com.hourblue.today.TodayMomentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminTodayMomentController.class)
@Import(SecurityConfiguration.class)
class AdminTodayMomentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TodayMomentService todayMomentService;

    @Test
    void anonymousUserCannotAccessTodayMomentAdmin() throws Exception {
        mockMvc.perform(get("/admin/today"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/login"));
    }

    @Test
    void authenticatedAdminCanGetPageWithDefaultDateCsrfCurrentAssignmentAndPublishedChoices() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 1);
        Post assigned = publishedPost(5L, "assigned");
        Post choice = publishedPost(6L, "choice");
        when(todayMomentService.today()).thenReturn(date);
        when(todayMomentService.findAssignment(date)).thenReturn(Optional.of(new TodayMoment(date, assigned)));
        when(todayMomentService.eligiblePosts()).thenReturn(List.of(assigned, choice));

        mockMvc.perform(get("/admin/today").with(user("admin@example.test")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Today's Moment")))
                .andExpect(content().string(containsString("value=\"2026-09-01\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("assigned title")))
                .andExpect(content().string(containsString("choice title")));
    }

    @Test
    void requestedDateIsRenderedAndInvalidDateIsSafe() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 2);
        when(todayMomentService.findAssignment(date)).thenReturn(Optional.empty());
        when(todayMomentService.today()).thenReturn(LocalDate.of(2026, 9, 1));
        when(todayMomentService.eligiblePosts()).thenReturn(List.of());

        mockMvc.perform(get("/admin/today")
                        .with(user("admin@example.test"))
                        .param("date", "2026-09-02"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"2026-09-02\"")));

        mockMvc.perform(get("/admin/today")
                        .with(user("admin@example.test"))
                        .param("date", "not-a-date"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Date must use YYYY-MM-DD.")))
                .andExpect(content().string(not(containsString("DateTimeParseException"))));
    }

    @Test
    void postAssignmentAndReplacementSucceed() throws Exception {
        mockMvc.perform(post("/admin/today")
                        .with(user("admin@example.test"))
                        .with(csrf())
                        .param("featureDate", "2026-09-01")
                        .param("postId", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/today?date=2026-09-01"));

        mockMvc.perform(post("/admin/today")
                        .with(user("admin@example.test"))
                        .with(csrf())
                        .param("featureDate", "2026-09-01")
                        .param("postId", "6"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/today?date=2026-09-01"));

        verify(todayMomentService).assign(LocalDate.of(2026, 9, 1), 5L);
        verify(todayMomentService).assign(LocalDate.of(2026, 9, 1), 6L);
    }

    @Test
    void assignmentRequiresCsrf() throws Exception {
        mockMvc.perform(post("/admin/today")
                        .with(user("admin@example.test"))
                        .param("featureDate", "2026-09-01")
                        .param("postId", "5"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidPostOrStatusErrorsRemainSafe() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 1);
        when(todayMomentService.findAssignment(date)).thenReturn(Optional.empty());
        when(todayMomentService.eligiblePosts()).thenReturn(List.of());
        when(todayMomentService.assign(date, 5L))
                .thenThrow(new InvalidTodayMomentException("Selected post must be published."));

        mockMvc.perform(post("/admin/today")
                        .with(user("admin@example.test"))
                        .with(csrf())
                        .param("featureDate", "2026-09-01")
                        .param("postId", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Selected post must be published.")))
                .andExpect(content().string(not(containsString("Duplicate entry"))))
                .andExpect(content().string(not(containsString("ConstraintViolationException"))));
    }

    @Test
    void persistenceFailureUsesSafeMessage() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 1);
        when(todayMomentService.findAssignment(date)).thenReturn(Optional.empty());
        when(todayMomentService.eligiblePosts()).thenReturn(List.of());
        when(todayMomentService.assign(date, 5L))
                .thenThrow(new InvalidTodayMomentException("Today's Moment could not be saved.",
                        new IllegalStateException("SQLState23000")));

        mockMvc.perform(post("/admin/today")
                        .with(user("admin@example.test"))
                        .with(csrf())
                        .param("featureDate", "2026-09-01")
                        .param("postId", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Today&#39;s Moment could not be saved.")))
                .andExpect(content().string(not(containsString("SQLState23000"))));
    }

    private Post publishedPost(Long id, String slug) {
        Post post = new Post(
                new Category("Design", "design", null),
                slug,
                slug + " title",
                slug + " description",
                "https://cdn.example.com/" + slug + ".jpg",
                slug + " alt");
        post.publish(Instant.parse("2026-09-01T00:00:00Z"));
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }
}
