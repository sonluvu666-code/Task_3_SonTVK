package com.test.demo;

class InvalidCredentialsException extends RuntimeException {

    InvalidCredentialsException() {
        super("The email address or password is incorrect.");
    }
}
