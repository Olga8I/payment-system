package org.example.acquiringserver.decoder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;


/**
 * Декодер преобразует бинарные данные в структурированную карту полей
 */
@Slf4j
@Component
public class TLVDecoder {

    /**
     * Парсинг TLV данных в карту полей
     */
    public Map<Byte, byte[]> parseTLV(byte[] tlvData) {
        Map<Byte, byte[]> fields = new HashMap<>();
        ByteArrayInputStream stream = new ByteArrayInputStream(tlvData);

        try {
            while (stream.available() > 0) {
                byte tag = (byte) stream.read();
                int length = ((stream.read() & 0xFF) << 8) | (stream.read() & 0xFF);
                byte[] value = new byte[length];
                int bytesRead = stream.read(value);

                if (bytesRead != length) {
                    throw new RuntimeException("Invalid TLV structure");
                }

                fields.put(tag, value);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse TLV data", e);
        }

        return fields;
    }

    public int fromMiddleEndian(byte[] bytes) {
        if (bytes.length != 4) {
            throw new IllegalArgumentException("Middle-endian conversion requires 4 bytes");
        }

        return ((bytes[1] & 0xFF) << 24) |
                ((bytes[0] & 0xFF) << 16) |
                ((bytes[3] & 0xFF) << 8)  |
                (bytes[2] & 0xFF);
    }
}