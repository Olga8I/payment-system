package org.example.posterminal.model;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Component
public class Transaction {
    private String pan;
    private int amount;
    private String transactionId;
    private String merchantId;
    private LocalDateTime timestamp;

    public Transaction() {
        this.transactionId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }

    public Transaction(String pan, int amount, String merchantId) {
        this();
        this.pan = pan;
        this.amount = amount;
        this.merchantId = merchantId;
    }

    @Override
    public String toString() {
        return String.format("Transaction{id=%s, pan=%s, amount=%d, merchant=%s}",
                transactionId, pan, amount, merchantId);
    }
}