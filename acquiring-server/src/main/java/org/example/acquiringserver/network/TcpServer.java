package org.example.acquiringserver.network;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.acquiringserver.service.PacketProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TcpServer {

    private final PacketProcessor packetProcessor;

    @Value("${server.port}")
    private int port;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private volatile boolean running = true;

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info(" Server started on port {}", port);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                log.debug("New client connected: {}", clientSocket.getInetAddress());

                executorService.submit(() -> handleClient(clientSocket));
            }
        }
    }

    @Async
    public void handleClient(Socket socket) {
        try (var input = socket.getInputStream();
             var output = socket.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead = input.read(buffer);

            if (bytesRead > 0) {
                log.debug("📨 Received {} bytes from client", bytesRead);
                byte[] response = packetProcessor.processPacket(buffer);
                output.write(response);
                log.debug("Sent {} bytes response", response.length);
            }

        } catch (IOException e) {
            log.error("Client handling error: {}", e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                log.warn("Error closing socket: {}", e.getMessage());
            }
        }
    }

}
