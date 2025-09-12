package org.example.acquiringserver.decoder;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Тесты для TLV декодера
 * Проверяет корректность парсинга TLV формата
 */
@SpringBootTest
class TLVDecoderTest {

    @Autowired
    private TLVDecoder tlvDecoder;

    @Test
    void testParseTLV_ValidData() {
        // Подготовка тестовых TLV данных
        byte[] tlvData = {
                0x10, 0x00, 0x10, // TAG=0x10, LENGTH=16
                '4','2','4','2','*','*','*','*','*','*','*','*','4','2','4','2', // PAN
                0x20, 0x00, 0x04, // TAG=0x20, LENGTH=4
                0x00, 0x00, 0x27, 0x10, // Amount = 10000 (100.00)
                0x30, 0x00, 0x24, // TAG=0x30, LENGTH=36
                't','e','s','t','-','u','u','i','d','-','1','2','3','4','5','6','7','8','9','0','1','2','3','4','5','6','7','8','9','0','1','2','3','4','5','6'
        };

        var result = tlvDecoder.parseTLV(tlvData);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsKey((byte) 0x10)); // PAN
        assertTrue(result.containsKey((byte) 0x20)); // Amount
        assertTrue(result.containsKey((byte) 0x30)); // Transaction ID
    }

    @Test
    void testBytesToIntBigEndian_ValidConversion() {
        byte[] amountBytes = {0x00, 0x00, 0x27, 0x10}; // 10000 в big-endian

        int result = tlvDecoder.bytesToIntBigEndian(amountBytes);

        assertEquals(10000, result);
    }

    @Test
    void testBytesToIntBigEndian_InvalidLength() {
        byte[] invalidBytes = {0x01, 0x02, 0x03}; // Всего 3 байта вместо 4

        assertThrows(IllegalArgumentException.class, () -> {
            tlvDecoder.bytesToIntBigEndian(invalidBytes);
        });
    }

    @Test
    void testParseTLV_EmptyData() {
        byte[] emptyData = {};

        assertThrows(RuntimeException.class, () -> {
            tlvDecoder.parseTLV(emptyData);
        });
    }
}