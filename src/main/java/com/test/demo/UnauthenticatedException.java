package com.test.demo;

class UnauthenticatedException extends RuntimeException {

    UnauthenticatedException() {
        super("Please sign in to continue.");
    }
}
