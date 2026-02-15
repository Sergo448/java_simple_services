package com.tutorial.hello;

import com.tutorial.hello.handlers.HealthHandler;
import com.tutorial.hello.handlers.HelloHandler;
import com.tutorial.hello.handlers.TimeHandler;
import com.tutorial.hello.handlers.MyMathHandler;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import com.sun.net.httpserver.HttpServer;
public class App {

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
            
            server.createContext("/hello", new HelloHandler());
            server.createContext("/time", new TimeHandler());
            server.createContext("/health", new HealthHandler());
            server.createContext("/math", new MyMathHandler());
            
            server.setExecutor(null); // creates a default executor
            server.start();
        } catch (Exception e) {
            e.printStackTrace();    
        }
    }
}
