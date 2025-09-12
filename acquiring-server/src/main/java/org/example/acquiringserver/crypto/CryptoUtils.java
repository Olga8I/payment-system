package org.example.acquiringserver.crypto;


import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Component
public class CryptoUtils {

    private static final String RSA_ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int GCM_TAG_LENGTH = 128;


    // Регистрация провайдера
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Value("${server.private.key}")
    private String privateKeyPath;

    @Value("${hmac.key}")
    private String hmacKey;

    private PrivateKey serverPrivateKey;


    /**
     * Инициализация компонентов после создания бина
     */
    @PostConstruct
    public void init() {
        try {
            this.serverPrivateKey = loadPrivateKeyFromClasspath(privateKeyPath);
            log.info("CryptoUtils initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize CryptoUtils: {}", e.getMessage());

            this.serverPrivateKey = createTempPrivateKey();
            log.warn("Using temporary private key for development");
        }
    }

    private PrivateKey loadPrivateKeyFromClasspath(String path) throws Exception {
        try {
            ClassPathResource resource = new ClassPathResource(path.replace("classpath:", ""));
            if (!resource.exists()) {
                throw new RuntimeException("Private key file not found: " + path);
            }

            byte[] keyBytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
            String privateKeyPem = new String(keyBytes);

            return parsePrivateKey(privateKeyPem);

        } catch (Exception e) {
            log.error("Failed to load private key from: {}", path);
            throw e;
        }
    }

    private PrivateKey parsePrivateKey(String privateKeyPem) throws Exception {
        String privateKeyContent = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * Создает временный приватный ключ
     */
    private PrivateKey createTempPrivateKey() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();
            return keyPair.getPrivate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temporary private key", e);
        }
    }

    /**
     * Дешифрование данных с помощью RSA
     */
    public byte[] decryptWithRSA(byte[] encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM, "BC");
            cipher.init(Cipher.DECRYPT_MODE, serverPrivateKey);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("RSA decryption failed", e);
        }
    }


    /**
     * Дешифрование данных с помощью AES-GCM
     */
    public byte[] decryptWithAES(byte[] encryptedData, byte[] key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("AES decryption failed", e);
        }
    }

    /**
     * Проверка HMAC подписи для обеспечения целостности данных
     */
    public boolean verifyHmac(byte[] data, byte[] expectedHmac) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(hmacKey.getBytes(), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] actualHmac = mac.doFinal(data);
            return MessageDigest.isEqual(actualHmac, expectedHmac);
        } catch (Exception e) {
            throw new RuntimeException("HMAC verification failed", e);
        }
    }
}