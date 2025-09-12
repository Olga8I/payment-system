package org.example.posterminal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.posterminal.service.TransactionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableRetry
@EnableScheduling
@RequiredArgsConstructor
public class PosTerminalApplication {

    private final TransactionService transactionService;

    public static void main(String[] args) {
        SpringApplication.run(PosTerminalApplication.class, args);
    }

    @Bean
    public CommandLineRunner run() {
        return args -> {
            log.info("Starting POS Terminal...");

            // Отправка 20 тестовых транзакций с интервалом 1 секунда
            for (int i = 0; i < 20; i++) {
                try {
                    transactionService.sendRandomTransaction();
                    Thread.sleep(1000);
                } catch (Exception e) {
                    log.error("Failed to send transaction: {}", e.getMessage());
                }
            }
        };
    }
}
