package com.example.candyland.network.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CandyLandServer authentication.
 */
class CandyLandServerAuthTest {
    
    @Test
    void testValidToken() {
        assertTrue(CandyLandServer.isValidToken("s3cret", "s3cret"));
    }
    
    @Test
    void testWrongToken() {
        assertFalse(CandyLandServer.isValidToken("s3cret", "wrong"));
    }
    
    @Test
    void testNullToken() {
        assertFalse(CandyLandServer.isValidToken("s3cret", null));
    }
    
    @Test
    void testBlankSecretRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CandyLandServer(8888, ""));
    }
    
    @Test
    void testNullSecretRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CandyLandServer(8888, null));
    }
}
