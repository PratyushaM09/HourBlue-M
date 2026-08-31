package com.hourblue.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class AdminUserDetailsServiceTests {

    @Test
    void enabledAdminLoadsWithStoredHash() {
        Admin admin = new Admin("admin@example.test", "$2a$10$fakeStoredHash");
        AdminRepository adminRepository = mock(AdminRepository.class);
        when(adminRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));

        UserDetails userDetails = new AdminUserDetailsService(adminRepository).loadUserByUsername(admin.getEmail());

        assertEquals(admin.getEmail(), userDetails.getUsername());
        assertEquals(admin.passwordHash(), userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
    }

    @Test
    void missingAdminThrowsUsernameNotFound() {
        AdminRepository adminRepository = mock(AdminRepository.class);
        when(adminRepository.findByEmail("missing@example.test")).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> new AdminUserDetailsService(adminRepository).loadUserByUsername("missing@example.test"));
    }
}
