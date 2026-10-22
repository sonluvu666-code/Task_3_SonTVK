package com.test.demo;

class InvalidPasswordException extends RuntimeException {

    InvalidPasswordException() {
        super("Password must be no more than 72 bytes when encoded as UTF-8.");
    }
}
