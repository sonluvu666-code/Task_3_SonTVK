package com.test.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class UserServiceTest {

    private JdbcUserRepository repository;
    private AccountRepository accountRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        repository = mock(JdbcUserRepository.class);
        accountRepository = mock(AccountRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(repository, accountRepository, passwordEncoder);
    }

    @Test
    void registerNormalizesEmailAndStoresAnEncodedPassword() {
        when(repository.findByName("Alex")).thenReturn(Optional.empty());
        when(repository.findByEmail("alex@example.com")).thenReturn(Optional.empty());
        when(repository.insert(eq(  "Alex"), eq("alex@example.com"), any(String.class)))
                .thenAnswer(invocation -> new User(1L, "Alex", "alex@example.com",
                        invocation.getArgument(2)));

        User user = userService.register(" Alex ", " Alex@Example.com ", "secret1");

        assertThat(user.email()).isEqualTo("alex@example.com");
        assertThat(user.passwordHash()).isNotEqualTo("secret1");
        assertThat(passwordEncoder.matches("secret1", user.passwordHash())).isTrue();
    }

    @Test
    void registerRejectsAnExistingName() {
        when(repository.findByName("Alex"))
                .thenReturn(Optional.of(new User(1L, "Alex", "alex@example.com", "hash")));

        assertThatThrownBy(() -> userService.register(" Alex ", "new@example.com", "secret1"))
                .isInstanceOf(DuplicateNameException.class);
    }

    @Test
    void registerRejectsAnExistingEmail() {
        when(repository.findByName("Alex")).thenReturn(Optional.empty());
        when(repository.findByEmail("alex@example.com"))
                .thenReturn(Optional.of(new User(1L, "Alex", "alex@example.com", "hash")));

        assertThatThrownBy(() -> userService.register("Alex", "alex@example.com", "secret1"))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void registerRejectsPasswordsExceedingBcryptByteLimit() {
        String password = "é".repeat(37);

        assertThatThrownBy(() -> userService.register("Alex", "alex@example.com", password))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void authenticateReturnsUserForMatchingPassword() {
        String hash = passwordEncoder.encode("secret1");
        User user = new User(1L, "Alex", "alex@example.com", hash);
        when(repository.findByEmail("alex@example.com")).thenReturn(Optional.of(user));

        assertThat(userService.authenticate("Alex@Example.com", "secret1")).isEqualTo(user);
        verify(accountRepository).recordLoginAttempt(1L, true);
    }

    @Test
    void authenticateRejectsIncorrectPassword() {
        when(repository.findByEmail("alex@example.com"))
                .thenReturn(Optional.of(new User(1L, "Alex", "alex@example.com",
                        passwordEncoder.encode("secret1"))));

        assertThatThrownBy(() -> userService.authenticate("alex@example.com", "wrong1"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(repository).findByEmail("alex@example.com");
        verify(accountRepository).recordLoginAttempt(1L, false);
    }

    @Test
    void updatePasswordRequiresCurrentPasswordAndStoresANewHash() {
        User user = new User(1L, "Alex", "alex@example.com", passwordEncoder.encode("secret1"));
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        userService.updatePassword(1L, "secret1", "newpass1");

        org.mockito.ArgumentCaptor<String> hash = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(repository).updatePassword(eq(1L), hash.capture());
        assertThat(passwordEncoder.matches("newpass1", hash.getValue())).isTrue();
    }

    @Test
    void updatePasswordRejectsAnIncorrectCurrentPassword() {
        when(repository.findById(1L)).thenReturn(Optional.of(new User(
                1L,
                "Alex",
                "alex@example.com",
                passwordEncoder.encode("secret1"))));

        assertThatThrownBy(() -> userService.updatePassword(1L, "wrong1", "newpass1"))
                .isInstanceOf(InvalidCredentialsException.class);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never())
                .updatePassword(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
    }
}
