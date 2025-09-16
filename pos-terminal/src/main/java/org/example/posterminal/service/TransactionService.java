package org.example.posterminal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.posterminal.crypto.CryptoUtils;
import org.example.posterminal.crypto.KeyRotationService;
import org.example.posterminal.encoder.HexDumpUtil;
import org.example.posterminal.encoder.TLVEncoder;
import org.example.posterminal.model.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * Сервис для управления транзакциями
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final int SOCKET_TIMEOUT_MS = 3000;
    private static final int ENCRYPTED_SESSION_KEY_SIZE = 256;
    private static final int IV_SIZE = 12;
    private static final int HMAC_SIZE = 32;

    private final CryptoUtils cryptoUtils;
    private final TLVEncoder tlvEncoder;
    private final KeyRotationService keyRotationService;
    private final SecureRandom secureRandom;
    private final TransactionGenerator transactionGenerator;

    @Value("${server.host}")
    private String serverHost;

    @Value("${server.port}")
    private int serverPort;

    /**
     * Генерация и отправка случайной транзакции
     */
    public void sendRandomTransaction() throws IOException {
        Transaction transaction = transactionGenerator.generateRandomTransaction();
        sendTransaction(transaction);
    }

    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 3000), include = SocketTimeoutException.class)
    public void sendTransaction(Transaction transaction) throws IOException {
        keyRotationService.incrementTransactionCount();
        log.info("Sending transaction: {}", transaction.getTransactionId());

        byte[] packet = createPacket(transaction);
        log.debug("Packet hexdump:\n{}", HexDumpUtil.toHexDump(packet));

        log.debug("Packet structure: header=4, sessionKey={}, iv={}, hmac={}, data={}",
                ENCRYPTED_SESSION_KEY_SIZE, IV_SIZE, HMAC_SIZE,
                (packet.length - 4 - ENCRYPTED_SESSION_KEY_SIZE - IV_SIZE - HMAC_SIZE));

        sendPacket(packet);
    }

    private byte[] createPacket(Transaction transaction) {
        try {
            byte[] sessionKey = cryptoUtils.generateSessionKey();
            byte[] encryptedSessionKey = cryptoUtils.encryptWithRSA(sessionKey);

            if (encryptedSessionKey.length != ENCRYPTED_SESSION_KEY_SIZE) {
                throw new RuntimeException("Encrypted session key has wrong size: " + encryptedSessionKey.length);
            }

            byte[] tlvData = tlvEncoder.encodeTransaction(transaction);

            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);

            byte[] encryptedData = cryptoUtils.encryptWithAES(tlvData, sessionKey, iv);
            byte[] hmac = cryptoUtils.calculateHmac(encryptedData);

            if (hmac.length != HMAC_SIZE) {
                throw new RuntimeException("HMAC has wrong size: " + hmac.length);
            }

            int totalLength = 4 + encryptedSessionKey.length + iv.length + hmac.length + encryptedData.length;

            ByteArrayOutputStream packet = new ByteArrayOutputStream(totalLength);

            packet.write(0x01); // версия
            packet.write(0x01); // тип сообщения
            packet.write((totalLength >> 8) & 0xFF); // старший байт длины
            packet.write(totalLength & 0xFF); // младший байт длины

            packet.write(encryptedSessionKey);
            packet.write(iv);
            packet.write(hmac);
            packet.write(encryptedData);

            byte[] result = packet.toByteArray();

            if (result.length != totalLength) {
                throw new RuntimeException("Packet length mismatch. Expected: " + totalLength + ", Actual: " + result.length);
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create packet", e);
        }
    }

    /**
     * Отправка бинарного пакета на сервер по TCP
     */
    private void sendPacket(byte[] packet) throws IOException {
        try (Socket socket = new Socket(serverHost, serverPort);
             OutputStream output = socket.getOutputStream();
             InputStream input = socket.getInputStream()) {

            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            output.write(packet);
            output.flush();

            byte[] response = new byte[1024];
            int bytesRead = input.read(response);

            if (bytesRead > 0) {
                processServerResponse(response, bytesRead);
            } else {
                throw new SocketTimeoutException("No response from server");
            }
        }
    }

    /**
     * Обработка ответа от сервера
     */
    private void processServerResponse(byte[] response, int length) {
        if (length < 15) {
            log.error("Invalid response length: {}", length);
            return;
        }

        ByteBuffer buffer = ByteBuffer.wrap(response, 0, length);
        byte status = buffer.get();

        byte[] authCodeBytes = new byte[6];
        buffer.get(authCodeBytes);
        String authCode = new String(authCodeBytes).trim();

        long timestamp = 0;
        for (int i = 0; i < 8; i++) {
            timestamp = (timestamp << 8) | (buffer.get() & 0xFF);
        }

        if (status == 0x00) {
            log.info("Transaction APPROVED. Auth code: {}, Time: {}",
                    authCode, Instant.ofEpochMilli(timestamp));
        } else if (status == 0x01) {
            log.info("Transaction DECLINED. Time: {}",
                    Instant.ofEpochMilli(timestamp));
        } else {
            log.warn("Server returned error status: {}", status);
        }
    }
}