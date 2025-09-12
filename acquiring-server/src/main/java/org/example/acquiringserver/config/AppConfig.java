package org.example.acquiringserver.config;


import lombok.extern.slf4j.Slf4j;
import org.example.acquiringserver.service.KeyGeneratorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class AppConfig {


    private final KeyGeneratorService keyGeneratorService;

    public AppConfig(KeyGeneratorService keyGeneratorService) {
        this.keyGeneratorService = keyGeneratorService;
    }

    @Bean
    public boolean checkKeys() {
        try {
            ClassPathResource resource = new ClassPathResource("server-private.pem");
            if (!resource.exists()) {
                log.warn("Private key file not found. Generating new key pair...");
                keyGeneratorService.generateAndSaveKeys();
            } else {
                log.info("Private key file found: server-private.pem");
            }
            return true;
        } catch (Exception e) {
            log.error("Key check failed: {}", e.getMessage());
            return false;
        }
    }

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("TCP-Processor-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Random random() {
        return new Random();
    }

    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }

    @Bean
    public Base64.Encoder base64Encoder() {
        return Base64.getEncoder();
    }

    @Bean
    public Base64.Decoder base64Decoder() {
        return Base64.getDecoder();
    }
}
