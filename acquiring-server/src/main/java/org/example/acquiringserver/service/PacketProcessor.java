package org.example.acquiringserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.acquiringserver.crypto.CryptoUtils;
import org.example.acquiringserver.decoder.TLVDecoder;
import org.example.acquiringserver.model.TransactionEntity;
import org.example.acquiringserver.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PacketProcessor {

    private final CryptoUtils cryptoUtils;
    private final TLVDecoder tlvDecoder;
    private final TransactionRepository repository;
    private final Random random;
    private final FailureEmulator failureEmulator;

    private static final int HEADER_SIZE = 4;
    private static final int ENCRYPTED_SESSION_KEY_SIZE = 256;
    private static final int IV_SIZE = 12;
    private static final int HMAC_SIZE = 32;

    public byte[] processPacket(byte[] receivedData) {
        try {
            failureEmulator.emulateNetworkDelay();

            // 1. Эмуляция таймаута (5%) - сервер не отвечает
            if (failureEmulator.shouldTimeout()) {
                log.warn("Emulating timeout (5% chance) - no response sent");
                return null; // Это приведет к закрытию соединения без ответа
            }

            ByteBuffer buffer = ByteBuffer.wrap(receivedData);

            // 2. Парсинг и валидация заголовка
            byte version = buffer.get();
            byte messageType = buffer.get();
            int totalPacketLength = Short.toUnsignedInt(buffer.getShort()); // Важно: длина ВСЕГО пакета

            if (version != 0x01) {
                log.warn("Unsupported protocol version: {}", version);
                return createErrorResponse(0x02, "UNSUPPORTED_VERSION");
            }
            if (messageType != 0x01) {
                log.warn("Unsupported message type: {}", messageType);
                return createErrorResponse(0x03, "UNSUPPORTED_TYPE");
            }
            if (totalPacketLength != receivedData.length) {
                log.warn("Packet length mismatch. Expected: {}, Actual: {}", totalPacketLength, receivedData.length);
                return createErrorResponse(0x04, "LENGTH_MISMATCH");
            }

            // 3. Эмуляция недоступности сервиса (2%)
            if (failureEmulator.shouldServiceBeUnavailable()) {
                return createErrorResponse(0x05, "SERVICE_UNAVAILABLE");
            }

            // 4. Извлечение компонентов пакета
            byte[] encryptedSessionKey = new byte[ENCRYPTED_SESSION_KEY_SIZE];
            buffer.get(encryptedSessionKey);

            byte[] iv = new byte[IV_SIZE];
            buffer.get(iv);

            byte[] receivedHmac = new byte[HMAC_SIZE];
            buffer.get(receivedHmac);

            // Все оставшиеся данные - зашифрованный TLV
            byte[] encryptedTlvData = new byte[buffer.remaining()];
            buffer.get(encryptedTlvData);

            // 5. Проверка HMAC
            if (!cryptoUtils.verifyHmac(encryptedTlvData, receivedHmac)) {
                log.warn("HMAC verification failed");
                return createErrorResponse(0x06, "HMAC_FAILED");
            }

            // 6. Эмуляция повреждения данных (0.5%)
            if (failureEmulator.shouldDataBeCorrupted()) {
                throw new RuntimeException("DATA_CORRUPTION_EMULATION");
            }

            // 7. Расшифровка
            byte[] sessionKey = cryptoUtils.decryptWithRSA(encryptedSessionKey);
            byte[] tlvData = cryptoUtils.decryptWithAES(encryptedTlvData, sessionKey, iv);

            // 8. Парсинг TLV
            Map<Byte, byte[]> fields = tlvDecoder.parseTLV(tlvData);
            TransactionEntity transaction = createTransactionEntity(fields);

            // 9. Эмуляция отказа банка (3%)
            boolean approved = !failureEmulator.shouldReject();
            transaction.setStatus(approved ? "APPROVED" : "DECLINED");

            // 10. Эмуляция сбоя БД (1%)
            if (failureEmulator.shouldDatabaseFail()) {
                throw new RuntimeException("DATABASE_FAILURE_EMULATION");
            }

            // 11. Сохранение и формирование ответа
            if (approved) {
                transaction.setAuthCode(generateAuthCode());
                repository.save(transaction);
                log.info("Transaction APPROVED: {}", transaction.getTransactionId());
                return createApprovalResponse(transaction.getAuthCode());
            } else {
                String declineReason = failureEmulator.getRandomDeclineReason();
                transaction.setAuthCode("DECLINED");
                transaction.setStatus("DECLINED_" + declineReason);
                repository.save(transaction);
                log.info("Transaction DECLINED: {} - {}", transaction.getTransactionId(), declineReason);
                return createDeclineResponse(declineReason);
            }

        } catch (Exception e) {
            log.error("Packet processing failed: {}", e.getMessage());
            return createErrorResponse(0x01, "PROCESSING_ERROR");
        }
    }

    private TransactionEntity createTransactionEntity(Map<Byte, byte[]> fields) {
        TransactionEntity transaction = new TransactionEntity();

        // PAN
        transaction.setPan(new String(fields.get((byte) 0x10)));
        // Amount (BIG-ENDIAN исправление!)
        transaction.setAmount(bytesToIntBigEndian(fields.get((byte) 0x20)));
        // Transaction ID
        transaction.setTransactionId(new String(fields.get((byte) 0x30)));
        // Merchant ID
        transaction.setMerchantId(new String(fields.get((byte) 0x40)));

        transaction.setTimestamp(LocalDateTime.now());
        return transaction;
    }

    /**
     * Преобразует байты в int (Big-Endian порядок)
     */
    private int bytesToIntBigEndian(byte[] bytes) {
        if (bytes.length != 4) {
            throw new IllegalArgumentException("Amount must be 4 bytes");
        }
        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8)  |
                (bytes[3] & 0xFF);
    }

    private String generateAuthCode() {
        return String.format("%06d", random.nextInt(1000000));
    }

    private byte[] createApprovalResponse(String authCode) {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        response.write(0x00);
        response.writeBytes(authCode.getBytes());
        writeTimestamp(response);
        return response.toByteArray();
    }

    private byte[] createDeclineResponse(String reason) {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        response.write(0x01);
        response.writeBytes("DECLIN".getBytes());
        writeTimestamp(response);
        return response.toByteArray();
    }

    private byte[] createErrorResponse(int errorCode, String errorMessage) {
        log.warn("Returning error response: {} - {}", errorCode, errorMessage);
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        response.write(errorCode);
        response.writeBytes("ERROR".getBytes());
        response.write(0x00);
        writeTimestamp(response);
        return response.toByteArray();
    }

    private void writeTimestamp(ByteArrayOutputStream stream) {
        long timestamp = System.currentTimeMillis();
        for (int i = 56; i >= 0; i -= 8) {
            stream.write((byte) ((timestamp >> i) & 0xFF));
        }
    }
}