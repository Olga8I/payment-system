package org.example.posterminal.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
/**
 * Сервис для управления ротацией криптографических ключей
 */
@Slf4j
@Service
public class KeyRotationService {

    private int transactionCount = 0;

    public void incrementTransactionCount() {
        transactionCount++;
        if (transactionCount % 10 == 0) {
            requestNewHmacKey();
        }
    }

    private void requestNewHmacKey() {
        log.info("Requesting new HMAC key (every 10 transactions)");
    }

    @Scheduled(fixedRate = 3600000)
    public void checkKeyValidity() {
        log.debug("Checking key validity...");
    }
}
