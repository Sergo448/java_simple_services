package com.tutorial.taskmanager.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tutorial.taskmanager.model.Task;
import com.tutorial.taskmanager.storage.TaskStorage;


public class TaskHandler implements HttpHandler {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final TaskStorage storage;

    public TaskHandler(TaskStorage storage) {
        this.storage = storage;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");

        boolean hasId = parts.length == 3 && !parts[2].isEmpty();


        if (hasId) {
            try {
                long id = Long.parseLong(parts[2]);
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    handleGetById(exchange, id);
                } else if ("PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                    handleUpdate(exchange, id);
                } else if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                    handleDelete(exchange, id);
                } else {
                    sendResponse(exchange, 405, buildMethodNotAllowed());
                }
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, buildBadRequest("Invalid ID format"));
            } catch (Exception e) {
                sendResponse(exchange, 500, buildInternalServerError(e.getMessage()));
            }
        } else {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleGetAll(exchange);
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCreate(exchange);
            } else {
                sendResponse(exchange, 405, buildMethodNotAllowed());
            }
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String body) 
        throws IOException {
            exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8"
            );
            byte[] responseBytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
                os.close();
            } catch (IOException e) {   
                e.printStackTrace();
            }
        }
    
    private String readRequestBody(HttpExchange exchange)
        throws IOException {
           try (InputStream is = exchange.getRequestBody()) {
                return new String(
                    is.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
           } catch (IOException e) {
                e.printStackTrace();
                throw e;
           }
        }

    private void handleUpdate (HttpExchange exchange, long id) 
        throws IOException {
            try {
                String body = readRequestBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                String title = json.has("title") ? json.get("title").getAsString() : null;
                String description = json.has("description") ? json.get("description").getAsString() : null;
                boolean completed = json.has("completed") ? json.get("completed").getAsBoolean() : false;

                Optional<Task> updated = storage.update(id, title, description, completed);
                if (updated.isPresent()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", id);
                    response.put("message", "Task updated successfully");
                    sendResponse(exchange, 200, gson.toJson(response));
                } else {
                    sendResponse(exchange, 404, buildNotFound());
                }
            } catch (IOException e) {
                sendResponse(exchange, 500, buildInternalServerError(e.getMessage()));
            }
        }

    private void handleGetAll (HttpExchange exchange)
        throws IOException {
            try {
                List<Task> tasks = storage.findAll();
                Map<String, Object> response = new HashMap<>();
                response.put("tasks", tasks);
                sendResponse(exchange, 200, gson.toJson(response));
            } catch (Exception e) {
                sendResponse(exchange, 500, buildInternalServerError(e.getMessage()));
            }
        }
    
    private void handleCreate (HttpExchange exchange) 
        throws IOException {
            try {
                String body = readRequestBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                String title = json.has("title") ? json.get("title").getAsString() : null;

                if (title == null || title.trim().isEmpty()) {
                    sendResponse(exchange, 400, buildBadRequest("Title is required"));
                    return;
                }

                String description = json.has("description") ? json.get("description").getAsString() : null;
                Task task = storage.create(title, description);
                Map<String, Object> response = new HashMap<>();
                response.put("id", task.getId());
                response.put("message", "Task created successfully");
                sendResponse(exchange, 201, gson.toJson(response));
            } catch (IOException e) {
                sendResponse(exchange, 500, buildInternalServerError(e.getMessage()));
            }
        }

    private void handleGetById (HttpExchange exchange, long id) 
        throws IOException {
            try {
                Optional<Task> task = storage.findById(id);
                if (task.isPresent()) {
                    sendResponse(exchange, 200, gson.toJson(task.get()));
                } else {
                    sendResponse(exchange, 404, buildNotFound());
                }
            } catch (IOException e) {
                sendResponse(exchange, 500, buildInternalServerError(e.getMessage()));
            }
        }

    private void handleDelete (HttpExchange exchange, long id)
        throws IOException {
            boolean deleted = storage.delete(id);
            if (deleted) {
                Map<String, Object> response = new HashMap<>();
                response.put("id", id);
                response.put("message", "Task deleted successfully");
                sendResponse(exchange, 200, gson.toJson(response));
            } else {
                sendResponse(exchange, 404, buildNotFound());
            }
        }

    private String buildMethodNotAllowed() {
        return """
                {
                  "error": "Method Not Allowed",
                  "message": "Supported methods: GET, POST, PUT, DELETE"
                }
                """;
        }
    private String buildBadRequest(String message) {
        return String.format("""
                {
                  "error": "Bad Request",
                  "message": "%s"
                }
                """, message);
    }
    private String buildNotFound() {
        return """
                {
                  "error": "Not Found",
                  "message": "The requested resource was not found"
                }
                """;
    }
    private String buildInternalServerError(String message) {
        return String.format("""
                {
                  "error": "Internal Server Error",
                  "message": "%s"
                }
                """, message);
    }
    private String buildInternalServerError() {
        return """
                {
                  "error": "Internal Server Error",
                  "message": "An unexpected error occurred"
                }
                """;
    }
    private String buildNotFound(String message) {
        return String.format("""
                {
                  "error": "Not Found",
                  "message": "%s"
                }
                """, message);
    }   
    private String buildMethodNotAllowed(String message) {
        return String.format("""
                {
                  "error": "Method Not Allowed",
                  "message": "%s"
                }
                """, message);
    }
    private String buildBadRequest() {
        return """
                {
                  "error": "Bad Request",
                  "message": "The request was invalid or cannot be processed"
                }
                """;
    }
}
