package com.test.demo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AccountRepository accountRepository;

    public UserController(UserService userService, AccountRepository accountRepository) {
        this.userService = userService;
        this.accountRepository = accountRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfile register(@Valid @RequestBody RegisterUserRequest request) {
        return UserProfile.from(userService.register(
                request.name(),
                request.email(),
                request.password()));
    }

    @PostMapping("/login")
    public UserProfile login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        User user = userService.authenticate(request.email(), request.password());
        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId();
        session.setAttribute(AccountController.SESSION_USER_ID, user.id());
        return UserProfile.from(user);
    }

    @GetMapping("/me")
    public UserProfile currentUser(HttpSession session) {
        return UserProfile.from(userService.findById(AccountController.requireUserId(session)));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession session) {
        session.invalidate();
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePassword(
            HttpSession session,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.updatePassword(
                AccountController.requireUserId(session),
                request.currentPassword(),
                request.newPassword());
    }

    @PostMapping(value = "/profile-image", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uploadProfileImage(
            HttpSession session,
            @RequestParam("image") MultipartFile image) throws IOException {
        accountRepository.storeProfileImage(
                AccountController.requireUserId(session),
                image.getBytes(),
                image.getContentType());
    }

    @GetMapping("/profile-image")
    public ResponseEntity<byte[]> profileImage(HttpSession session) {
        return accountRepository.findProfileImage(AccountController.requireUserId(session))
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(image.contentType()))
                        .header("Cache-Control", "private, no-store")
                        .body(image.data()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record RegisterUserRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 6, max = 72) String password) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank String password) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 6, max = 72) String newPassword) {
    }

    public record UserProfile(Long id, String name, String email) {

        static UserProfile from(User user) {
            return new UserProfile(user.id(), user.name(), user.email());
        }
    }
}
