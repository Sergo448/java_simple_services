package com.tutorial.taskmanager;

import java.net.http.HttpClient;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.io.IOException;
import com.tutorial.taskmanager.storage.TaskStorage;
import com.tutorial.taskmanager.handlers.TaskHandler;


public class App {

    public static void main(String[] args) {
        HttpServer server;
        try {
            server = HttpServer.create(new java.net.InetSocketAddress(8082), 0);
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return; 
        }
        TaskStorage taskStorage = new TaskStorage();
        server.createContext("/tasks", new TaskHandler(taskStorage));

        server.setExecutor(null); // creates a default executor
        server.start();

        System.out.println("Task Manager Service is running on port 8082...");

    }
}
