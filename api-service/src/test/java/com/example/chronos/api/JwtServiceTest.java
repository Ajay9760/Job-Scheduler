package com.example.chronos.api;

import static org.junit.jupiter.api.Assertions.*;

import com.example.chronos.api.security.JwtService;
import com.example.chronos.domain.User;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void roundTripAndTamper() {
        JwtService s = new JwtService(SECRET, 5);
        User u = new User();
        u.setUsername("ajay");
        u.setTokenVersion(2);
        String t = s.issue(u);
        var c = s.parse(t).orElseThrow();
        assertEquals("ajay", c.getSubject());
        assertEquals(2, c.get("tv", Integer.class));
        assertTrue(s.parse(t + "x").isEmpty());
        assertTrue(new JwtService("another-secret-another-secret-123456", 5).parse(t).isEmpty());
    }

    @Test
    void shortSecretRejected() {
        assertThrows(IllegalStateException.class, () -> new JwtService("short", 5));
    }
}
