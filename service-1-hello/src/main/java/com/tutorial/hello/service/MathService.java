package com.tutorial.hello.service;

public class MathService {

    public double calculate(double a, double b, String operation) {

        double result;

        if (operation == "+") {
            result = a + b;
        } else if (operation == "-") {
            result = a - b;
        } else if (operation == "*") {
            result = a * b;
        } else if (operation == "/") {
            if (b == 0 || b == 0.0 || b == Double.NaN || b == Double.POSITIVE_INFINITY || b == Double.NEGATIVE_INFINITY) {
                throw new IllegalArgumentException("Division by zero");
            }
            result = a / b;
        }
        else {
            throw new IllegalArgumentException("Unsupported operation");
        }
        return result;
    }
}
