package com.raxrot.back.utils;

import com.raxrot.back.models.User;
import com.raxrot.back.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthUtilTest {

    private UserRepository userRepository;
    private AuthUtil authUtil;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authUtil = new AuthUtil(userRepository);

        var authentication = new UsernamePasswordAuthenticationToken("john", "pwd");
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loggedInEmail_returnsEmail() {
        User user = new User();
        user.setUserId(42L);
        user.setUserName("john");
        user.setEmail("john@mail.com");

        when(userRepository.findByUserName("john")).thenReturn(Optional.of(user));

        String email = authUtil.loggedInEmail();
        assertEquals("john@mail.com", email);
    }

    @Test
    void loggedInUserId_returnsId() {
        User user = new User();
        user.setUserId(42L);
        user.setUserName("john");
        user.setEmail("john@mail.com");

        when(userRepository.findByUserName("john")).thenReturn(Optional.of(user));

        Long id = authUtil.loggedInUserId();
        assertEquals(42L, id);
    }

    @Test
    void loggedInUser_returnsUser() {
        User user = new User();
        user.setUserId(42L);
        user.setUserName("john");
        user.setEmail("john@mail.com");

        when(userRepository.findByUserName("john")).thenReturn(Optional.of(user));

        User found = authUtil.loggedInUser();
        assertEquals("john", found.getUserName());
        assertEquals(42L, found.getUserId());
    }

    @Test
    void throwsWhenUserNotFound() {
        when(userRepository.findByUserName("john")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authUtil.loggedInUser());
    }
}