package com.raxrot.back.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthEntryPointJwtTest {

    @Test
    void commence_writes_401_json_body() throws Exception {
        AuthEntryPointJwt entry = new AuthEntryPointJwt();

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/private");
        req.setServletPath("/api/private");
        MockHttpServletResponse res = new MockHttpServletResponse();

        entry.commence(req, res, new BadCredentialsException("Bad credentials"));

        assertEquals(401, res.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, res.getContentType());

        Map<?,?> body = new ObjectMapper().readValue(res.getContentAsByteArray(), Map.class);
        assertEquals(401, body.get("status"));
        assertEquals("Unauthorized", body.get("error"));
        assertEquals("/api/private", body.get("path"));
        assertTrue(String.valueOf(body.get("message")).contains("Bad"));
    }
}