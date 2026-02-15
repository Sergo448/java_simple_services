package com.tutorial.hello.handlers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HelloHandler implements HttpHandler {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, buildMethodNotAllowed());
            return;
        }

        long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;

        String hostname = InetAddress.getLocalHost().getHostName();

        Map<String, Object> responce = Map.of(
                "message", "Hello, World!",
                "service", "hello-service",
                "hostname", hostname,
                "uptimeSeconds", uptimeSeconds,
                "memory", Map.of(
                        "usedMB", usedMemory,
                        "freeMB", freeMemory,
                        "totalMB", totalMemory
                )
        );

        String jsonResponse = gson.toJson(responce);   

        sendJson(exchange, 200, jsonResponse);
    }

    private String buildMethodNotAllowed() {
        return """
                {
                  "error": "Method Not Allowed",
                  "message": "Only GET method is supported for /hello"
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
