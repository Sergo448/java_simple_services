package com.tutorial.userservice.exception;

import java.util.HashMap;
import jakarta.persistence.Entity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;




@RestControllerAdvice
public class GlobalExceptionHandler {
    // @ExceptionHandler(UserNotFoundException.class) → 404
    // @ExceptionHandler(MethodArgumentNotValidException.class) → 400

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFoundException(
        UserNotFoundException ex) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse.toString());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(
        MethodArgumentNotValidException ex) {
            Map<String, String> errorResponse = new HashMap<>();
            ex.getBindingResult().getAllErrors().forEach(error -> 
                errorResponse.put(
                    error.getField(), 
                    error.getDefaultMessage()
                )
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse.toString());
    }
}
