package com.test.demo;

class InvalidImageException extends RuntimeException {

    InvalidImageException() {
        super("Choose a PNG, JPEG, or GIF image no larger than 5 MB with dimensions up to 4096 pixels.");
    }
}
