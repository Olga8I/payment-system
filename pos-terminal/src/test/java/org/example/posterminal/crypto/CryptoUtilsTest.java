package org.example.posterminal.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CryptoUtilsTest {

    @Autowired
    private CryptoUtils cryptoUtils;

    private byte[] testData;

    @BeforeEach
    void setUp() {
        testData = "Test data for encryption".getBytes();
    }

    @Test
    void testSessionKeyGeneration() {
        byte[] sessionKey = cryptoUtils.generateSessionKey();
        assertNotNull(sessionKey);
        assertEquals(32, sessionKey.length);
    }

    @Test
    void testAesEncryptionDecryption() {
        byte[] sessionKey = cryptoUtils.generateSessionKey();
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        byte[] encrypted = cryptoUtils.encryptWithAES(testData, sessionKey, iv);
        assertNotNull(encrypted);
        assertTrue(encrypted.length > 0);
    }

    @Test
    void testHmacConsistency() {
        byte[] hmac1 = cryptoUtils.calculateHmac(testData);
        byte[] hmac2 = cryptoUtils.calculateHmac(testData);

        assertNotNull(hmac1);
        assertEquals(32, hmac1.length);
        assertArrayEquals(hmac1, hmac2);
    }
}
