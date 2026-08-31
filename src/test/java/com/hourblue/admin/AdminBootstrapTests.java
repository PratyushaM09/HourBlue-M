package com.hourblue.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminBootstrapTests {

    @Test
    void createsAdminWhenCredentialsArePresentAndNoAdminExists() {
        AdminRepository adminRepository = mock(AdminRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(adminRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("plain-test-password")).thenReturn("encoded-password");

        new AdminBootstrap(
                adminRepository,
                passwordEncoder,
                " Admin@Example.TEST ",
                "plain-test-password").run(null);

        ArgumentCaptor<Admin> admin = ArgumentCaptor.forClass(Admin.class);
        verify(passwordEncoder).encode("plain-test-password");
        verify(adminRepository).save(admin.capture());
        assertEquals("admin@example.test", admin.getValue().getEmail());
        assertEquals("encoded-password", admin.getValue().passwordHash());
        assertTrue(admin.getValue().isEnabled());
    }

    @Test
    void missingOrIncompleteCredentialsCreateNothing() {
        assertBootstrapDoesNothing("", "plain-test-password");
        assertBootstrapDoesNothing("admin@example.test", "");
        assertBootstrapDoesNothing(" ", "plain-test-password");
        assertBootstrapDoesNothing("admin@example.test", " ");
    }

    @Test
    void existingAdminPreventsBootstrapCreation() {
        AdminRepository adminRepository = mock(AdminRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(adminRepository.count()).thenReturn(1L);

        new AdminBootstrap(adminRepository, passwordEncoder, "new@example.test", "new-password").run(null);

        verify(adminRepository).count();
        verifyNoMoreInteractions(adminRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void changedCredentialsDoNotUpdateExistingAdmin() {
        AdminRepository adminRepository = mock(AdminRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(adminRepository.count()).thenReturn(1L);

        new AdminBootstrap(adminRepository, passwordEncoder, "changed@example.test", "changed-password").run(null);

        verify(adminRepository).count();
        verifyNoMoreInteractions(adminRepository);
        verifyNoInteractions(passwordEncoder);
    }

    private void assertBootstrapDoesNothing(String email, String password) {
        AdminRepository adminRepository = mock(AdminRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        new AdminBootstrap(adminRepository, passwordEncoder, email, password).run(null);

        verifyNoInteractions(adminRepository, passwordEncoder);
    }
}
