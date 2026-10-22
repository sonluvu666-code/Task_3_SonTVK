package com.test.demo;

class TodoNotFoundException extends RuntimeException {

    TodoNotFoundException() {
        super("The requested task was not found.");
    }
}
