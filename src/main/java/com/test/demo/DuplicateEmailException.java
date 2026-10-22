package com.test.demo;

class DuplicateEmailException extends RuntimeException {

    DuplicateEmailException() {
        super("An account with this email address already exists.");
    }

    DuplicateEmailException(Throwable cause) {
        super("An account with this email address already exists.", cause);
    }
}
