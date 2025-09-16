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

    private static final int ENCRYPTED_SESSION_KEY_SIZE = 256;
    private static final int IV_SIZE = 12;
    private static final int HMAC_SIZE = 32;
    private static final int HEADER_SIZE = 4;

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            log.info("Server started on port {}", port);

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

            byte[] header = new byte[4];
            int headerBytesRead = input.read(header);

            if (headerBytesRead != 4) {
                log.error("Invalid header size: {}", headerBytesRead);
                return;
            }

            int totalPacketLength = ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);

            int remainingLength = totalPacketLength - 4;
            byte[] remainingData = new byte[remainingLength];
            int bytesRead = input.read(remainingData);

            if (bytesRead != remainingLength) {
                log.error("Incomplete packet. Expected: {}, Got: {}", remainingLength, bytesRead);
                return;
            }

            byte[] fullPacket = new byte[totalPacketLength];
            System.arraycopy(header, 0, fullPacket, 0, 4);
            System.arraycopy(remainingData, 0, fullPacket, 4, remainingLength);

            log.debug("Received {} bytes from client", totalPacketLength);
            byte[] response = packetProcessor.processPacket(fullPacket);

            if (response != null) {
                output.write(response);
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