package org.example.acquiringserver.decoder;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * Тесты для TLV декодера
 */
@SpringBootTest
class TLVDecoderTest {

    @Autowired
    private TLVDecoder tlvDecoder;

    @Test
    void testParseTLV_ValidData() {

        byte[] tlvData = {
                0x10, 0x00, 0x10,
                '4','2','4','2','*','*','*','*','*','*','*','*','4','2','4','2',
                0x20, 0x00, 0x04,
                0x00, 0x00, 0x27, 0x10,
                0x30, 0x00, 0x24,
                't','e','s','t','-','u','u','i','d','-','1','2','3','4','5','6','7','8','9','0','1','2','3','4','5','6','7','8','9','0','1','2','3','4','5','6'
        };

        var result = tlvDecoder.parseTLV(tlvData);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsKey((byte) 0x10));
        assertTrue(result.containsKey((byte) 0x20));
        assertTrue(result.containsKey((byte) 0x30));
    }

    @Test
    void testFromMiddleEndian_ValidConversion() {
        byte[] amountBytes = {0x00, 0x00, 0x27, 0x10};

        int result = tlvDecoder.fromMiddleEndian(amountBytes);

        assertEquals(10000, result);
    }

    @Test
    void testFromMiddleEndian_InvalidLength() {
        byte[] invalidBytes = {0x01, 0x02, 0x03};

        assertThrows(IllegalArgumentException.class, () -> {
            tlvDecoder.fromMiddleEndian(invalidBytes);
        });
    }

    @Test
    void testParseTLV_EmptyData() {
        byte[] emptyData = {};

        assertThrows(RuntimeException.class, () -> {
            tlvDecoder.parseTLV(emptyData);
        });
    }

    @Test
    void testParseTLV_IncompleteData() {

        byte[] incompleteData = {0x10, 0x00};

        assertThrows(RuntimeException.class, () -> {
            tlvDecoder.parseTLV(incompleteData);
        });
    }

    @Test
    void testParseTLV_CorruptedLength() {
        byte[] corruptedData = {
                0x10, 0x00, 0x05,
                '1', '2', '3'
        };

        assertThrows(RuntimeException.class, () -> {
            tlvDecoder.parseTLV(corruptedData);
        });
    }
}