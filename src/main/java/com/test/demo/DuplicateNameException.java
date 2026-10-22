package com.test.demo;

class DuplicateNameException extends RuntimeException {

    DuplicateNameException() {
        super("An account with this name already exists.");
    }

    DuplicateNameException(Throwable cause) {
        super("An account with this name already exists.", cause);
    }
}
