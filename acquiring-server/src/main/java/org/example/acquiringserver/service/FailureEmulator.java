package org.example.acquiringserver.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
public class FailureEmulator {

    private final Random random;

    public FailureEmulator(Random random) {
        this.random = random;
    }

    /**
     * Эмуляция 5% таймаутов (потеря пакетов)
     */
    public boolean shouldTimeout() {
        boolean timeout = random.nextDouble() < 0.05;
        if (timeout) {
            log.warn("Emulating timeout (5% chance) - packet will be dropped");
        }
        return timeout;
    }

    /**
     * Эмуляция 3% отклонений банком-эмитентом
     */
    public boolean shouldReject() {
        boolean reject = random.nextDouble() < 0.03;
        if (reject) {
            log.warn("Emulating bank rejection (3% chance) - transaction declined");
        }
        return reject;
    }

    public void emulateNetworkDelay() {
        try {
            int delay = random.nextInt(101); // случайная задержка от 0 до 100ms
            if (delay > 0) {
                log.debug("Emulating network delay: {}ms", delay);
                Thread.sleep(delay);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Network delay emulation interrupted");
        }
    }

    public boolean shouldDatabaseFail() {
        boolean dbFail = random.nextDouble() < 0.01;
        if (dbFail) {
            log.error("Emulating database failure (1% chance)");
        }
        return dbFail;
    }

    public boolean shouldServiceBeUnavailable() {
        boolean unavailable = random.nextDouble() < 0.02;
        if (unavailable) {
            log.error("Emulating service unavailable (2% chance)");
        }
        return unavailable;
    }

    public String getRandomDeclineReason() {
        String[] declineReasons = {
                "INSUFFICIENT_FUNDS",
                "CARD_EXPIRED",
                "TRANSACTION_LIMIT_EXCEEDED",
                "SUSPICIOUS_ACTIVITY",
                "CARD_BLOCKED",
                "INVALID_MERCHANT",
                "TECHNICAL_ERROR"
        };
        return declineReasons[random.nextInt(declineReasons.length)];
    }

    public boolean shouldDataBeCorrupted() {
        boolean corrupted = random.nextDouble() < 0.005;
        if (corrupted) {
            log.error("Emulating data corruption (0.5% chance)");
        }
        return corrupted;
    }
}