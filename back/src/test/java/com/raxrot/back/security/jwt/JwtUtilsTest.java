package com.raxrot.back.security.jwt;

import com.raxrot.back.security.services.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    private static final String TEST_B64_SECRET = "ZmFrZVNlY3JldEtleUZvclRlc3QxMjM0NTY3ODkwMTIzNDU2Nzg5MA==";
    private static final String COOKIE_NAME = "springBootEcom";

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", TEST_B64_SECRET);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3600_000); // 1h
        ReflectionTestUtils.setField(jwtUtils, "jwtCookie", COOKIE_NAME);
    }

    @Test
    void generate_and_parse_token_ok() {
        String token = jwtUtils.generateTokenFromUsername("alice");
        assertNotNull(token);

        String subject = jwtUtils.getUserNameFromJwtToken(token);
        assertEquals("alice", subject);

        assertTrue(jwtUtils.validateJwtToken(token));
    }

    @Test
    void expired_token_is_invalid() {
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", -1000);
        String expired = jwtUtils.generateTokenFromUsername("bob");

        assertFalse(jwtUtils.validateJwtToken(expired));
        assertThrows(ExpiredJwtException.class, () ->
                jwtUtils.getUserNameFromJwtToken(expired));
    }

    @Test
    void cookie_helpers_ok() {
        UserDetailsImpl principal = new UserDetailsImpl(1L, "john", "john@mail.test", "pwd", null);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 600_000);
        ResponseCookie cookie = jwtUtils.getJwtCookie(principal);

        assertEquals(COOKIE_NAME, cookie.getName());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("/api", cookie.getPath());
        assertEquals("Lax", cookie.getSameSite());

        ResponseCookie clean = jwtUtils.getCleanJwtCookie();
        assertEquals(0, clean.getMaxAge().getSeconds());
        assertEquals("", clean.getValue());

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie(COOKIE_NAME, "abc.jwt.value"));
        assertEquals("abc.jwt.value", jwtUtils.getJwtFromCookies(req));

        MockHttpServletRequest reqNo = new MockHttpServletRequest();
        assertNull(jwtUtils.getJwtFromCookies(reqNo));
    }
}