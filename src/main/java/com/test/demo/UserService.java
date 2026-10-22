package com.test.demo;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final JdbcUserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            JdbcUserRepository userRepository,
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String name, String email, String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidPasswordException();
        }
        String normalizedName = name.trim();
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.findByName(normalizedName).isPresent()) {
            throw new DuplicateNameException();
        }
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateEmailException();
        }

        try {
            return userRepository.insert(
                    normalizedName,
                    normalizedEmail,
                    passwordEncoder.encode(password));
        } catch (DuplicateKeyException exception) {
            if (userRepository.findByName(normalizedName).isPresent()) {
                throw new DuplicateNameException(exception);
            }
            if (userRepository.findByEmail(normalizedEmail).isPresent()) {
                throw new DuplicateEmailException(exception);
            }
            throw exception;
        }
    }

    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(InvalidCredentialsException::new);
        boolean valid = password.getBytes(StandardCharsets.UTF_8).length <= 72
                && passwordEncoder.matches(password, user.passwordHash());
        accountRepository.recordLoginAttempt(user.id(), valid);
        if (!valid) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    public User findById(long id) {
        return userRepository.findById(id).orElseThrow(UnauthenticatedException::new);
    }

    public void updatePassword(long id, String currentPassword, String newPassword) {
        if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidPasswordException();
        }
        User user = findById(id);
        if (currentPassword.getBytes(StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(currentPassword, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        userRepository.updatePassword(id, passwordEncoder.encode(newPassword));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
