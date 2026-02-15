package com.tutorial.hello.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tutorial.hello.model.MathRequest;
import com.tutorial.hello.model.MathResponse;
import com.tutorial.hello.service.MathService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class MyMathHandler implements HttpHandler {

    private final MathService mathService = new MathService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, """
                { "error": "Only POST is allowed" }
            """);
            return;
        }

        try (InputStream is = exchange.getRequestBody()) {

            MathRequest request =
                    objectMapper.readValue(is, MathRequest.class);

            double result = mathService.calculate(
                    request.a,
                    request.b,
                    request.operation
            );

            MathResponse response = new MathResponse(result);

            long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory() / (1024 * 1024);
            long freeMemory = runtime.freeMemory() / (1024 * 1024);
            long usedMemory = totalMemory - freeMemory;

            String hostname = InetAddress.getLocalHost().getHostName();

            String jsonResponse = """
                {
                  "response": "%s",
                  "service": "math-service",
                  "hostname": "%s",
                  "uptimeSeconds": %d,
                  "memory": {
                    "usedMB": %d,
                    "freeMB": %d,
                    "totalMB": %d
                  }
                }
                """.formatted(
                response,
                hostname,
                uptimeSeconds,
                usedMemory,
                freeMemory,
                totalMemory
        );

            sendJson(exchange, 200, jsonResponse);

        } catch (Exception e) {

            sendJson(exchange, 400, """
                { "error": "Invalid request" }
            """);
        }
    }

    private void sendJson(HttpExchange exchange,
                          int status,
                          String json) throws IOException {

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
            os.close();
        } catch (IOException e) {
            // Handle potential IOException during response writing
            System.err.println("Error sending response: " + e.getMessage());
        }
    }
}
