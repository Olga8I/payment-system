package org.example.posterminal.crypto;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Component
public class CryptoUtils {

    private static final String RSA_ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int AES_KEY_SIZE = 256;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Value("${server.public.key:classpath:server-public.pem}")
    private String serverPublicKeyPath;

    @Value("${hmac.key:my-secret-hmac-key-12345}")
    private String hmacKey;

    private PublicKey serverPublicKey;

    /**
     * Инициализация криптографических компонентов
     */
    @PostConstruct
    public void init() {
        try {
            this.serverPublicKey = loadPublicKey(serverPublicKeyPath);
            log.info("CryptoUtils initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize CryptoUtils: {}", e.getMessage());
            throw new RuntimeException("Crypto initialization failed. Please check server-public.pem file", e);
        }
    }

    /**
     * Генерация сессионного AES ключа
     */
    public byte[] generateSessionKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_SIZE);
            return keyGen.generateKey().getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate session key", e);
        }
    }

    public byte[] encryptWithRSA(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM, "BC");
            cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("RSA encryption failed", e);
        }
    }

    public byte[] encryptWithAES(byte[] data, byte[] key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    public byte[] calculateHmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(hmacKey.getBytes(), HMAC_ALGORITHM);
            mac.init(keySpec);
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("HMAC calculation failed", e);
        }
    }

    /**
     * Загрузка публичного ключа сервера из файла
     */
    private PublicKey loadPublicKey(String path) throws Exception {
        try {
            String cleanPath = path.replace("classpath:", "");
            ClassPathResource resource = new ClassPathResource(cleanPath);

            if (!resource.exists()) {
                throw new RuntimeException("Public key file not found: " + cleanPath);
            }

            byte[] keyBytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
            String publicKeyPem = new String(keyBytes);

            return parsePublicKey(publicKeyPem);

        } catch (Exception e) {
            log.error("Failed to load public key from: {}", path);
            throw e;
        }
    }

    /**
     * Парсинг публичного ключа
     */
    private PublicKey parsePublicKey(String publicKeyPem) throws Exception {
        String publicKeyContent = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(publicKeyContent);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }
}