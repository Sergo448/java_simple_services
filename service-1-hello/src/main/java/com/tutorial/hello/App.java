package com.tutorial.hello;

import com.tutorial.hello.handlers.ApiConsoleHandler;
import com.tutorial.hello.handlers.HealthHandler;
import com.tutorial.hello.handlers.HelloHandler;
import com.tutorial.hello.handlers.TimeHandler;
import com.tutorial.hello.handlers.MyMathHandler;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class App {

    private static final int PORT = 8081;

    public static void main(String[] args) {
        try {
            // Создаем сервер на порту 8081
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Регистрируем обработчики для разных путей
            server.createContext("/", new ApiConsoleHandler());
            server.createContext("/hello", new HelloHandler());
            server.createContext("/time", new TimeHandler());
            server.createContext("/health", new HealthHandler());
            server.createContext("/math", new MyMathHandler());

            // Используем ThreadPoolExecutor с фиксированным числом потоков
            server.setExecutor(Executors.newFixedThreadPool(10));

            // Логируем запуск сервера
            System.out.println("Server started on port " + PORT);
            server.start();

            // Добавляем shutdown hook для корректной остановки
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Stopping server...");
                server.stop(0);
                System.out.println("Server stopped");
            }));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
