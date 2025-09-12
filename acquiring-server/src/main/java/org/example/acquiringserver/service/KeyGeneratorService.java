package org.example.acquiringserver.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

@Slf4j
@Service
public class KeyGeneratorService {

    public void generateAndSaveKeys() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();

            // Сохраняем приватный ключ
            String privateKeyPem = convertToPem(keyPair.getPrivate().getEncoded(), "PRIVATE KEY");
            saveKeyToFile(privateKeyPem, "server-private.pem");

            // Сохраняем публичный ключ (для клиента)
            String publicKeyPem = convertToPem(keyPair.getPublic().getEncoded(), "PUBLIC KEY");
            saveKeyToFile(publicKeyPem, "server-public.pem");

            log.info("RSA key pair generated successfully");

        } catch (Exception e) {
            log.error("Failed to generate keys: {}", e.getMessage());
            throw new RuntimeException("Key generation failed", e);
        }
    }

    private void saveKeyToFile(String keyContent, String filename) {
        try {
            Path path = Paths.get("src/main/resources/" + filename);
            Files.createDirectories(path.getParent());
            Files.write(path, keyContent.getBytes());
            log.info("Key saved to: {}", path);
        } catch (IOException e) {
            log.error("Failed to save key to file: {}", e.getMessage());
        }
    }

    private String convertToPem(byte[] keyBytes, String type) {
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        return "-----BEGIN " + type + "-----\n" +
                base64Key.replaceAll("(.{64})", "$1\n") +
                "\n-----END " + type + "-----";
    }
}