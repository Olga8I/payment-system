package org.example.acquiringserver.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pan", nullable = false, length = 19)
    private String pan;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "transaction_id", unique = true, nullable = false, length = 36)
    private String transactionId;

    @Column(name = "merchant_id", nullable = false, length = 50)
    private String merchantId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "auth_code", length = 20)
    private String authCode;

    @Column(name = "decline_reason", length = 50)
    private String declineReason;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "protocol_version")
    private Byte protocolVersion;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}