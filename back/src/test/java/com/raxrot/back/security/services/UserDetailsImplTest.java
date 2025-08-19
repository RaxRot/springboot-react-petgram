package com.raxrot.back.security.services;

import com.raxrot.back.models.AppRole;
import com.raxrot.back.models.Role;
import com.raxrot.back.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserDetailsImplTest {

    @Test
    void build_shouldMapUserFieldsAndRoles() {
        Role role = new Role();
        role.setRoleName(AppRole.ROLE_USER);

        User user = new User();
        user.setUserId(1L);
        user.setUserName("john");
        user.setEmail("john@mail.com");
        user.setPassword("secret");
        user.setRoles(Set.of(role));

        UserDetailsImpl details = UserDetailsImpl.build(user);

        assertEquals(1L, details.getId());
        assertEquals("john", details.getUsername());
        assertEquals("john@mail.com", details.getEmail());
        assertEquals("secret", details.getPassword());

        assertEquals(1, details.getAuthorities().size());
        GrantedAuthority auth = details.getAuthorities().iterator().next();
        assertEquals("ROLE_USER", auth.getAuthority());
    }

    @Test
    void equals_shouldCompareById() {
        UserDetailsImpl u1 = new UserDetailsImpl(1L, "john", "a@a.com", "pwd", null);
        UserDetailsImpl u2 = new UserDetailsImpl(1L, "jack", "b@b.com", "pwd2", null);

        assertEquals(u1, u2);
    }
}