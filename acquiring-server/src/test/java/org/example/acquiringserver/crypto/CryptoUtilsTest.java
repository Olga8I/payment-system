package org.example.acquiringserver.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CryptoUtilsTest {

    @Autowired
    private CryptoUtils cryptoUtils;

    private byte[] testData;

    @BeforeEach
    void setUp() {
        testData = "test data for hmac verification".getBytes();
    }

    @Test
    void testHmacVerificationWithCorrectHmac() {
        byte[] testHmac = createTestHmacManually(testData);

        assertTrue(cryptoUtils.verifyHmac(testData, testHmac),
                "HMAC verification should pass with correct HMAC");
    }

    @Test
    void testHmacVerificationFailure() {
        byte[] wrongHmac = "completely_wrong_hmac_value_123".getBytes();

        assertFalse(cryptoUtils.verifyHmac(testData, wrongHmac),
                "HMAC verification should fail with incorrect HMAC");
    }

    @Test
    void testHmacVerificationWithNullData() {
        byte[] testHmac = createTestHmacManually(testData);

        assertThrows(Exception.class, () -> {
            cryptoUtils.verifyHmac(null, testHmac);
        }, "Should throw exception with null data");
    }

    @Test
    void testHmacVerificationWithNullHmac() {
        assertThrows(Exception.class, () -> {
            cryptoUtils.verifyHmac(testData, null);
        }, "Should throw exception with null HMAC");
    }

    @Test
    void testHmacVerificationWithEmptyData() {
        byte[] emptyData = new byte[0];
        byte[] testHmac = createTestHmacManually(emptyData);

        assertTrue(cryptoUtils.verifyHmac(emptyData, testHmac),
                "HMAC verification should work with empty data");
    }

    private byte[] createTestHmacManually(byte[] data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec keySpec =
                    new javax.crypto.spec.SecretKeySpec("my-secret-hmac-key-12345".getBytes(), "HmacSHA256");
            mac.init(keySpec);
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test HMAC", e);
        }
    }
}