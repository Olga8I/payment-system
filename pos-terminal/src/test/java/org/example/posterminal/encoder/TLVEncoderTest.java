package org.example.posterminal.encoder;

import org.example.posterminal.model.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TLVEncoderTest {

    @Autowired
    private TLVEncoder tlvEncoder;

    @Test
    void testTransactionEncoding() {
        Transaction transaction = new Transaction("4242********4242", 1000, "TEST_MERCHANT");

        byte[] tlvData = tlvEncoder.encodeTransaction(transaction);

        assertNotNull(tlvData);
        assertTrue(tlvData.length > 0);
    }

    @Test
    void testMiddleEndianConversion() {
        int originalValue = 0x12345678;
        byte[] middleEndian = tlvEncoder.toMiddleEndian(originalValue);

        assertEquals(4, middleEndian.length);
        assertEquals(0x34, middleEndian[0] & 0xFF);
        assertEquals(0x12, middleEndian[1] & 0xFF);
        assertEquals(0x78, middleEndian[2] & 0xFF);
        assertEquals(0x56, middleEndian[3] & 0xFF);
    }
}