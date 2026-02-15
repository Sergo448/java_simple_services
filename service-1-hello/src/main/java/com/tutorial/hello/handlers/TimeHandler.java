package com.tutorial.hello.handlers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class TimeHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, buildMethodNotAllowed());
            return;
        }

        // Текущие дата и время
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        String dateString = now.format(dateFormatter);
        String timeString = now.format(timeFormatter);
        String dateTimeString = now.toString(); // ISO формат: 2026-02-15T14:30:00

        // Uptime
        long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        // Память
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;

        // Hostname
        String hostname = InetAddress.getLocalHost().getHostName();

        // JSON ответ
        String jsonResponse = """
                {
                  "message": "Hello, World!",
                  "currentDateTime": "%s",
                  "currentDate": "%s",
                  "currentTime": "%s",
                  "service": "time-service",
                  "hostname": "%s",
                  "uptimeSeconds": %d,
                  "memory": {
                    "usedMB": %d,
                    "freeMB": %d,
                    "totalMB": %d
                  }
                }
                """.formatted(
                dateTimeString,
                dateString,
                timeString,
                hostname,
                uptimeSeconds,
                usedMemory,
                freeMemory,
                totalMemory
        );

        sendJson(exchange, 200, jsonResponse);
    }

    private String buildMethodNotAllowed() {
        return """
                {
                  "error": "Method Not Allowed",
                  "message": "Only GET method is supported for /time"
                }
                """;
    }

    private void sendJson(HttpExchange exchange, int statusCode, String json)
            throws IOException {

        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
            os.close();
        } catch (IOException e) {
            // Handle potential IOException during response writing
            System.err.println("Error sending response: " + e.getMessage());
        }
    }
}
