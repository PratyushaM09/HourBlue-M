package com.hourblue.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.hourblue.admin.Admin;
import com.hourblue.admin.AdminRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.transaction.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void adminLoginPageIsPublicAndContainsFormFields() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Admin sign in")))
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void adminLoginPageShowsGenericErrorAndLogoutMessages() throws Exception {
        mockMvc.perform(get("/admin/login?error"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid email or password.")))
                .andExpect(content().string(not(containsString("disabled"))));

        mockMvc.perform(get("/admin/login?logout"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You have been signed out.")));
    }

    @Test
    void healthRemainsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedAdminRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/login"));
    }

    @Test
    void directAnonymousAdminRouteGuessesRedirectToLogin() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "imageFile",
                "post.jpg",
                "image/jpeg",
                new byte[] {1});

        assertRedirectsToLogin(get("/admin"));
        assertRedirectsToLogin(get("/admin/categories"));
        assertRedirectsToLogin(post("/admin/categories").with(csrf()));
        assertRedirectsToLogin(get("/admin/posts"));
        assertRedirectsToLogin(get("/admin/posts/new"));
        assertRedirectsToLogin(multipart("/admin/posts").file(image).with(csrf()));
        assertRedirectsToLogin(get("/admin/posts/5/edit"));
        assertRedirectsToLogin(post("/admin/posts/5").with(csrf()));
        assertRedirectsToLogin(post("/admin/posts/5/publish").with(csrf()));
        assertRedirectsToLogin(post("/admin/posts/5/archive").with(csrf()));
        assertRedirectsToLogin(multipart("/admin/posts/5/image").file(image).with(csrf()));
        assertRedirectsToLogin(get("/admin/today"));
        assertRedirectsToLogin(post("/admin/today").with(csrf()));
    }

    @Test
    void successfulFormLoginCreatesAuthenticatedAccess() throws Exception {
        Credentials admin = admin();

        MvcResult login = mockMvc.perform(formLogin("/admin/login").user(admin.email()).password(admin.password()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(authenticated().withUsername(admin.email()))
                .andReturn();

        mockMvc.perform(get("/admin").session(session(login)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("HourBlue admin")))
                .andExpect(content().string(containsString(admin.email())));
    }

    @Test
    void authenticatedAdminVisitingLoginRedirectsToAdmin() throws Exception {
        mockMvc.perform(get("/admin/login").with(user("admin@example.test")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void failedFormLoginDoesNotRevealKnownUnknownOrMalformedAdmin() throws Exception {
        Credentials admin = admin();

        assertLoginFailsGenerically(admin.email(), "wrong-test-password");
        assertLoginFailsGenerically(email(), "wrong-test-password");
        assertLoginFailsGenerically("not-an-email", "wrong-test-password");
    }

    @Test
    void disabledAdminCannotLogIn() throws Exception {
        Credentials admin = disabledAdmin();

        mockMvc.perform(formLogin("/admin/login").user(admin.email()).password(admin.password()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void logoutRequiresPostAndRedirects() throws Exception {
        Credentials admin = admin();
        MvcResult login = mockMvc.perform(formLogin("/admin/login").user(admin.email()).password(admin.password()))
                .andReturn();
        MockHttpSession session = session(login);

        mockMvc.perform(get("/admin/logout").session(session))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/admin/logout").with(csrf()).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?logout"))
                .andExpect(unauthenticated());

        mockMvc.perform(get("/admin").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/login"));
    }

    @Test
    void loginAndLogoutPostRequireCsrf() throws Exception {
        Credentials admin = admin();

        mockMvc.perform(post("/admin/login")
                        .param("username", admin.email())
                        .param("password", admin.password()))
                .andExpect(status().isForbidden());

        MvcResult login = mockMvc.perform(formLogin("/admin/login").user(admin.email()).password(admin.password()))
                .andReturn();

        mockMvc.perform(post("/admin/logout").session(session(login)))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedPostRequiresCsrf() throws Exception {
        Credentials admin = admin();
        MvcResult login = mockMvc.perform(formLogin("/admin/login").user(admin.email()).password(admin.password()))
                .andReturn();
        MockHttpSession session = session(login);

        mockMvc.perform(post("/admin/test").session(session))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/test").with(csrf()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string("posted"));
    }

    @Test
    void publicNonAdminRouteIsNotBlocked() throws Exception {
        mockMvc.perform(get("/public-test"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));
    }

    @Test
    void publicResponsesIncludeSecurityHeaders() throws Exception {
        mockMvc.perform(get("/public-test"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Content-Security-Policy", SecurityConfiguration.CONTENT_SECURITY_POLICY))
                .andExpect(header().string("Permissions-Policy", SecurityConfiguration.PERMISSIONS_POLICY));
    }

    @Test
    void generatedLoginPageIsNoLongerAvailable() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCssIsPublic() throws Exception {
        mockMvc.perform(get("/css/admin.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("focus-visible")));
    }

    private Credentials admin() {
        String password = "test-password-" + unique();
        String email = email();
        adminRepository.saveAndFlush(new Admin(email, passwordEncoder.encode(password)));
        return new Credentials(email, password);
    }

    private Credentials disabledAdmin() {
        String password = "test-password-" + unique();
        String email = email();
        jdbcTemplate.update(
                "insert into admins (email, password_hash, enabled, created_at, updated_at) values (?, ?, false, current_timestamp(6), current_timestamp(6))",
                email,
                passwordEncoder.encode(password));
        return new Credentials(email, password);
    }

    private String email() {
        return "admin-" + unique() + "@example.test";
    }

    private String unique() {
        return UUID.randomUUID().toString();
    }

    private MockHttpSession session(MvcResult result) {
        return (MockHttpSession) result.getRequest().getSession();
    }

    private void assertRedirectsToLogin(RequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/login"));
    }

    private void assertLoginFailsGenerically(String email, String password) throws Exception {
        mockMvc.perform(formLogin("/admin/login").user(email).password(password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error"))
                .andExpect(unauthenticated());

        mockMvc.perform(get("/admin/login?error"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid email or password.")))
                .andExpect(content().string(not(containsString(email))));
    }

    private record Credentials(String email, String password) {
    }

    @TestConfiguration
    static class TestRoutes {

        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {

        @GetMapping("/admin/test")
        String admin() {
            return "admin";
        }

        @PostMapping("/admin/test")
        String adminPost() {
            return "posted";
        }

        @GetMapping("/public-test")
        String publicRoute() {
            return "public";
        }
    }
}
