package com.hourblue.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import com.hourblue.category.CategoryRepository;
import com.hourblue.post.PostRepository;
import com.hourblue.post.PostStatus;
import com.hourblue.publicsite.PublicPostController;
import com.hourblue.seo.SiteUrlBuilder;
import com.hourblue.subscriber.Subscriber;
import com.hourblue.subscriber.SubscriberRepository;
import com.hourblue.subscriber.SubscriptionController;
import com.hourblue.subscriber.SubscriptionService;
import com.hourblue.today.TodayMomentService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

@WebMvcTest({PublicPostController.class, SubscriptionController.class})
@Import({SecurityConfiguration.class, SiteUrlBuilder.class, SubscriptionService.class})
@TestPropertySource(properties = "hourblue.site-base-url=https://hourblue.example")
class SubscriptionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostRepository postRepository;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private TodayMomentService todayMomentService;

    @MockitoBean
    private SubscriberRepository subscriberRepository;

    @MockitoBean
    private PlatformTransactionManager transactionManager;

    private final TransactionStatus transactionStatus = org.mockito.Mockito.mock(TransactionStatus.class);

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
    }

    @Test
    void homepageContainsSubscriptionForm() throws Exception {
        homepage();

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Join the HourBlue list")))
                .andExpect(content().string(containsString("action=\"/subscribe\"")))
                .andExpect(content().string(containsString("<footer")))
                .andExpect(content().string(containsString("href=\"/privacy\"")))
                .andExpect(content().string(containsString("href=\"/terms\"")))
                .andExpect(content().string(containsString("href=\"/unsubscribe\"")))
                .andExpect(content().string(containsString("name=\"email\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void anonymousVisitorCanSubscribeWithCsrf() throws Exception {
        mockMvc.perform(post("/subscribe")
                        .with(csrf())
                        .param("email", "visitor@example.test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("subscriptionSuccessMessage", "You're on the HourBlue list."));

        ArgumentCaptor<Subscriber> subscriber = ArgumentCaptor.forClass(Subscriber.class);
        verify(subscriberRepository).saveAndFlush(subscriber.capture());
        org.junit.jupiter.api.Assertions.assertEquals("visitor@example.test", subscriber.getValue().getEmail());
    }

    @Test
    void existingSubscriptionUsesSameSuccessUx() throws Exception {
        when(subscriberRepository.existsByEmail("visitor@example.test")).thenReturn(true);

        MvcResult result = mockMvc.perform(post("/subscribe")
                        .with(csrf())
                        .param("email", "  Visitor@Example.TEST  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("subscriptionSuccessMessage", "You're on the HourBlue list."))
                .andReturn();
        homepage();

        mockMvc.perform(get("/").flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You&#39;re on the HourBlue list.")))
                .andExpect(content().string(not(containsString("visitor@example.test"))));
        verify(subscriberRepository, never()).saveAndFlush(any());
    }

    @Test
    void unsubscribePageIsPublicAndContainsFormFields() throws Exception {
        mockMvc.perform(get("/unsubscribe"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Email removal")))
                .andExpect(content().string(containsString("action=\"/unsubscribe\"")))
                .andExpect(content().string(containsString("name=\"email\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void privacyAndTermsPagesArePublicAndRenderTitles() throws Exception {
        mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Privacy Policy - HourBlue</title>")))
                .andExpect(content().string(containsString("<h1>Privacy Policy</h1>")));

        mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Terms of Service - HourBlue</title>")))
                .andExpect(content().string(containsString("<h1>Terms of Service</h1>")));
    }

    @Test
    void legalPagesLinkUnsubscribeAndUseConfiguredCanonicalUrl() throws Exception {
        mockMvc.perform(get("/privacy").header("Host", "attacker.example"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/unsubscribe\"")))
                .andExpect(content().string(containsString("rel=\"canonical\" href=\"https://hourblue.example/privacy\"")))
                .andExpect(content().string(not(containsString("attacker.example"))));

        mockMvc.perform(get("/terms").header("Host", "attacker.example"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/unsubscribe\"")))
                .andExpect(content().string(containsString("rel=\"canonical\" href=\"https://hourblue.example/terms\"")))
                .andExpect(content().string(not(containsString("attacker.example"))));
    }

    @Test
    void legalPagesUsePublicFooter() throws Exception {
        mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<footer")))
                .andExpect(content().string(containsString("href=\"/privacy\"")))
                .andExpect(content().string(containsString("href=\"/terms\"")))
                .andExpect(content().string(containsString("href=\"/unsubscribe\"")));
    }

    @Test
    void unsubscribeRequiresCsrf() throws Exception {
        mockMvc.perform(post("/unsubscribe").param("email", "visitor@example.test"))
                .andExpect(status().isForbidden());

        verify(subscriberRepository, never()).deleteByEmail(any());
    }

    @Test
    void existingAndUnknownUnsubscribeUseSameSuccessUx() throws Exception {
        when(subscriberRepository.deleteByEmail("existing@example.test")).thenReturn(1);
        when(subscriberRepository.deleteByEmail("unknown@example.test")).thenReturn(0);

        MvcResult existing = unsubscribe("existing@example.test");
        MvcResult unknown = unsubscribe("unknown@example.test");

        assertUnsubscribeSuccess(existing, "existing@example.test");
        assertUnsubscribeSuccess(unknown, "unknown@example.test");
    }

    @Test
    void unsubscribeNormalizesEmailLikeSubscribe() throws Exception {
        unsubscribe("  Visitor@Example.TEST  ");

        verify(subscriberRepository).deleteByEmail("visitor@example.test");
    }

    @Test
    void malformedUnsubscribeEmailIsHandledSafelyWithoutReflectingInput() throws Exception {
        String submittedEmail = "not-an-email";

        MvcResult result = mockMvc.perform(post("/unsubscribe")
                        .with(csrf())
                        .param("email", submittedEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/unsubscribe"))
                .andExpect(flash().attribute("unsubscribeErrorMessage", "Enter a valid email address."))
                .andReturn();

        mockMvc.perform(get("/unsubscribe").flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enter a valid email address.")))
                .andExpect(content().string(not(containsString(submittedEmail))));
        verify(subscriberRepository, never()).deleteByEmail(any());
    }

    @Test
    void oversizedUnsubscribeEmailIsHandledSafelyWithoutReflectingInput() throws Exception {
        String submittedEmail = "a".repeat(309) + "@example.test";

        MvcResult result = mockMvc.perform(post("/unsubscribe")
                        .with(csrf())
                        .param("email", submittedEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/unsubscribe"))
                .andExpect(flash().attributeExists("unsubscribeErrorMessage"))
                .andReturn();

        mockMvc.perform(get("/unsubscribe").flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(submittedEmail))));
        verify(subscriberRepository, never()).deleteByEmail(any());
    }

    @Test
    void subscriptionRequiresCsrf() throws Exception {
        mockMvc.perform(post("/subscribe").param("email", "visitor@example.test"))
                .andExpect(status().isForbidden());

        verify(subscriberRepository, never()).saveAndFlush(any());
    }

    @Test
    void successMessageAppearsAfterRedirect() throws Exception {
        MvcResult result = mockMvc.perform(post("/subscribe")
                        .with(csrf())
                        .param("email", "visitor@example.test"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        homepage();

        mockMvc.perform(get("/").flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You&#39;re on the HourBlue list.")));
    }

    @Test
    void blankEmailIsHandledSafely() throws Exception {
        MvcResult result = mockMvc.perform(post("/subscribe")
                        .with(csrf())
                        .param("email", " "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("subscriptionErrorMessage", "Email is required."))
                .andReturn();
        homepage();

        mockMvc.perform(get("/").flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Email is required.")));
        verify(subscriberRepository, never()).saveAndFlush(any());
    }

    @Test
    void malformedEmailIsHandledSafelyWithoutReflectingInput() throws Exception {
        String submittedEmail = "not-an-email";

        MvcResult result = mockMvc.perform(post("/subscribe")
                        .with(csrf())
                        .param("email", submittedEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("subscriptionErrorMessage", "Enter a valid email address."))
                .andReturn();
        homepage();

        mockMvc.perform(get("/").flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enter a valid email address.")))
                .andExpect(content().string(not(containsString(submittedEmail))));
        verify(subscriberRepository, never()).saveAndFlush(any());
    }

    @Test
    void oversizedEmailIsHandledSafelyWithoutReflectingInput() throws Exception {
        String submittedEmail = "a".repeat(309) + "@example.test";

        MvcResult result = mockMvc.perform(post("/subscribe")
                        .with(csrf())
                        .param("email", submittedEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("subscriptionErrorMessage"))
                .andReturn();
        homepage();

        mockMvc.perform(get("/").flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(submittedEmail))));
        verify(subscriberRepository, never()).saveAndFlush(any());
    }

    @Test
    void duplicateRaceProducesSameSuccessUxWithoutRenderingDiagnostic() throws Exception {
        String diagnostic = "SQLState23000 duplicate key";
        when(subscriberRepository.existsByEmail("duplicate@example.test")).thenReturn(false, true);
        when(subscriberRepository.saveAndFlush(any(Subscriber.class)))
                .thenThrow(new DataIntegrityViolationException(diagnostic));

        MvcResult result = mockMvc.perform(post("/subscribe")
                        .with(csrf())
                        .param("email", "duplicate@example.test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("subscriptionSuccessMessage", "You're on the HourBlue list."))
                .andReturn();
        homepage();

        mockMvc.perform(get("/").flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You&#39;re on the HourBlue list.")))
                .andExpect(content().string(not(containsString(diagnostic))));
    }

    private void homepage() {
        when(todayMomentService.resolveTodayMoment()).thenReturn(Optional.empty());
        when(postRepository.findAllByStatusOrderByPublishedAtDesc(
                org.mockito.Mockito.eq(PostStatus.PUBLISHED),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
    }

    private MvcResult unsubscribe(String email) throws Exception {
        return mockMvc.perform(post("/unsubscribe")
                        .with(csrf())
                        .param("email", email))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/unsubscribe"))
                .andExpect(flash().attribute(
                        "unsubscribeSuccessMessage",
                        "If that email was subscribed, it has been removed."))
                .andReturn();
    }

    private void assertUnsubscribeSuccess(MvcResult result, String email) throws Exception {
        mockMvc.perform(get("/unsubscribe").flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("If that email was subscribed, it has been removed.")))
                .andExpect(content().string(not(containsString(email))));
    }
}
