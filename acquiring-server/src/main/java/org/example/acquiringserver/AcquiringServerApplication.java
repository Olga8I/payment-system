package org.example.acquiringserver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.acquiringserver.network.TcpServer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class AcquiringServerApplication {

    private final TcpServer tcpServer;

    public static void main(String[] args) {
        SpringApplication.run(AcquiringServerApplication.class, args);
    }

    @Bean
    public CommandLineRunner run() {
        return args -> {
            try {
                tcpServer.start();
            } catch (Exception e) {
                log.error("Failed to start TCP server: {}", e.getMessage());
                System.exit(1);
            }
        };
    }
}
