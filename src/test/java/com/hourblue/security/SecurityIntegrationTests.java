package com.hourblue.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
    void healthRemainsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedAdminRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void successfulFormLoginCreatesAuthenticatedAccess() throws Exception {
        Credentials admin = admin();

        MvcResult login = mockMvc.perform(formLogin().user(admin.email()).password(admin.password()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(authenticated().withUsername(admin.email()))
                .andReturn();

        mockMvc.perform(get("/admin/test").session((org.springframework.mock.web.MockHttpSession) login.getRequest().getSession()))
                .andExpect(status().isOk())
                .andExpect(content().string("admin"));
    }

    @Test
    void failedFormLoginRedirectsToError() throws Exception {
        mockMvc.perform(formLogin().user(email()).password("wrong-test-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void disabledAdminCannotLogIn() throws Exception {
        Credentials admin = disabledAdmin();

        mockMvc.perform(formLogin().user(admin.email()).password(admin.password()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void logoutRequiresPostAndRedirects() throws Exception {
        Credentials admin = admin();
        MvcResult login = mockMvc.perform(formLogin().user(admin.email()).password(admin.password()))
                .andReturn();
        org.springframework.mock.web.MockHttpSession session =
                (org.springframework.mock.web.MockHttpSession) login.getRequest().getSession();

        mockMvc.perform(get("/admin/logout").session(session))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/admin/logout").with(csrf()).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(unauthenticated());

        mockMvc.perform(get("/admin/test").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/login"));        
    }

    @Test
    void protectedPostRequiresCsrf() throws Exception {
        Credentials admin = admin();
        MvcResult login = mockMvc.perform(formLogin().user(admin.email()).password(admin.password()))
                .andReturn();
        org.springframework.mock.web.MockHttpSession session =
                (org.springframework.mock.web.MockHttpSession) login.getRequest().getSession();

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
