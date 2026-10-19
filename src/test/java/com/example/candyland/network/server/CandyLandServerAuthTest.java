package com.example.candyland.network.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CandyLandServer authentication checks.
 */
class CandyLandServerAuthTest {
    
    @Test
    void testMatchingTokenIsAuthorized() {
        assertTrue(CandyLandServer.isAuthorized("s3cret", "s3cret"));
    }
    
    @Test
    void testInvalidTokensAreRejected() {
        assertFalse(CandyLandServer.isAuthorized("s3cret", "wrong"));
        assertFalse(CandyLandServer.isAuthorized("s3cret", null));
        assertFalse(CandyLandServer.isAuthorized(null, "s3cret"));
        assertFalse(CandyLandServer.isAuthorized("s3cret", ""));
    }
    
    @Test
    void testConstructorRejectsMissingSecret() {
        assertThrows(IllegalArgumentException.class, () -> new CandyLandServer(8888, ""));
        assertThrows(IllegalArgumentException.class, () -> new CandyLandServer(8888, null));
    }
}
