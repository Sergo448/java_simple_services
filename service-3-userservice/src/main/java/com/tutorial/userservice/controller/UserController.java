package com.tutorial.userservice.controller;

import org.springframework.web.bind.annotation.RestController;

import com.tutorial.userservice.model.User;
import com.tutorial.userservice.service.UserService;

import javax.annotation.processing.Generated;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.persistence.Entity;
import jakarta.validation.Valid;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;




@RestController
@RequestMapping("/api/users")
public class UserController {
    //   GET    /api/users       → список пользователей
    //   GET    /api/users/{id}  → пользователь по ID
    //   POST   /api/users       → создание
    //   PUT    /api/users/{id}  → обновление
    //   DELETE /api/users/{id}  → удаление

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.status(200).ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(
        @PathVariable Long id) {
            User user = userService.getUserById(id);
            return ResponseEntity.status(200).ok(user);
        }

    @PostMapping("/")
    public ResponseEntity<User> createUser(
        @Valid @RequestBody User user) {
            User createdUser = userService.createUser(user);
            return ResponseEntity.status(201).body(createdUser);
        }
    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody User user) {
            User updatedUser = userService.updateUser(id, user);
            return ResponseEntity.status(200).body(updatedUser);
        }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
        @PathVariable Long id) {
            userService.deleteUser(id);
            return ResponseEntity.status(204).build();
        }
}
