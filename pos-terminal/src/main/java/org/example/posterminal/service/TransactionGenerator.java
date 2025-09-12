package org.example.posterminal.service;

import lombok.extern.slf4j.Slf4j;
import org.example.posterminal.model.Transaction;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Генератор тестовых транзакций
 */
@Slf4j
@Service
public class TransactionGenerator {

    private final Random random = new Random();
    private final String[] merchants = {"MERCHANT_001", "MERCHANT_002", "MERCHANT_003"};
    private final String[] pans = {"4242********4242", "5555********5555", "3782********0005"};

    public Transaction generateRandomTransaction() {
        String pan = pans[random.nextInt(pans.length)];
        int amount = 100 + random.nextInt(9900);
        String merchant = merchants[random.nextInt(merchants.length)];

        return new Transaction(pan, amount, merchant);
    }
}
