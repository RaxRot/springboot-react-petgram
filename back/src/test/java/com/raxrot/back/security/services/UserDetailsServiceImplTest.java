package com.raxrot.back.security.services;

import com.raxrot.back.models.User;
import com.raxrot.back.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserDetailsServiceImpl service = new UserDetailsServiceImpl();

    {
        service.userRepository = userRepository;
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetails() {
        User user = new User();
        user.setUserId(1L);
        user.setUserName("john");
        user.setEmail("john@mail.com");
        user.setPassword("pwd");

        when(userRepository.findByUserName("john")).thenReturn(Optional.of(user));

        var details = service.loadUserByUsername("john");

        assertEquals("john", details.getUsername());
        assertEquals("pwd", details.getPassword());
    }

    @Test
    void loadUserByUsername_shouldThrowWhenNotFound() {
        when(userRepository.findByUserName("nope")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("nope"));
    }
}
