package com.raxrot.back.security.jwt;

import com.raxrot.back.security.services.UserDetailsImpl;
import com.raxrot.back.security.services.UserDetailsServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthTokenFilterTest {

    private JwtUtils jwtUtils;
    private UserDetailsServiceImpl userDetailsService;
    private AuthTokenFilter filter;

    @BeforeEach
    void setup() {
        jwtUtils = mock(JwtUtils.class);
        userDetailsService = mock(UserDetailsServiceImpl.class);

        filter = new AuthTokenFilter();
        org.springframework.test.util.ReflectionTestUtils.setField(filter, "jwtUtils", jwtUtils);
        org.springframework.test.util.ReflectionTestUtils.setField(filter, "userDetailsService", userDetailsService);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sets_authentication_when_token_valid() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/secure");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtUtils.getJwtFromCookies(req)).thenReturn("valid.jwt");
        when(jwtUtils.validateJwtToken("valid.jwt")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("valid.jwt")).thenReturn("john");

        UserDetailsImpl user = new UserDetailsImpl(1L, "john", "john@mail", "pwd",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername("john")).thenReturn(user);

        filter.doFilter(req, res, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("john", SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
    }

    @Test
    void leaves_context_empty_when_no_token_or_invalid() throws Exception {
        MockHttpServletRequest req1 = new MockHttpServletRequest("GET", "/api/secure");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        MockFilterChain chain1 = new MockFilterChain();

        when(jwtUtils.getJwtFromCookies(req1)).thenReturn(null);

        filter.doFilter(req1, res1, chain1);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        MockHttpServletRequest req2 = new MockHttpServletRequest("GET", "/api/secure");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        MockFilterChain chain2 = new MockFilterChain();

        when(jwtUtils.getJwtFromCookies(req2)).thenReturn("bad.jwt");
        when(jwtUtils.validateJwtToken("bad.jwt")).thenReturn(false);

        filter.doFilter(req2, res2, chain2);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void does_not_break_on_exception_inside_try() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/secure");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtUtils.getJwtFromCookies(req)).thenThrow(new RuntimeException("boom"));

        assertDoesNotThrow(() -> filter.doFilter(req, res, chain));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}