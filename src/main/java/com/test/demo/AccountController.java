package com.test.demo;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@Validated
public class AccountController {

    static final String SESSION_USER_ID = "accountUserId";

    private final AccountRepository accountRepository;
    private final ExcelExportService excelExportService;
    private final UserService userService;

    public AccountController(
            AccountRepository accountRepository,
            ExcelExportService excelExportService,
            UserService userService) {
        this.accountRepository = accountRepository;
        this.excelExportService = excelExportService;
        this.userService = userService;
    }

    static long requireUserId(HttpSession session) {
        Object userId = session.getAttribute(SESSION_USER_ID);
        if (userId instanceof Long id && id > 0) {
            return id;
        }
        throw new UnauthenticatedException();
    }

    @GetMapping("/login-history")
    public List<AccountRepository.LoginHistoryEntry> loginHistory(HttpSession session) {
        return accountRepository.findLoginHistory(requireUserId(session));
    }

    @GetMapping("/login-history.xlsx")
    public ResponseEntity<byte[]> exportLoginHistory(HttpSession session) throws IOException {
        long userId = requireUserId(session);
        User user = userService.findById(userId);
        byte[] export = excelExportService.loginHistory(
                accountRepository.findLoginHistory(userId),
                user.name(),
                user.email());
        return excelResponse("login-history.xlsx", export);
    }

    @GetMapping("/todos")
    public List<AccountRepository.TodoItem> todos(HttpSession session) {
        return accountRepository.findTodos(requireUserId(session));
    }

    @PostMapping("/todos")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountRepository.TodoItem addTodo(
            HttpSession session,
            @Valid @RequestBody CreateTodoRequest request) {
        return accountRepository.addTodo(
                requireUserId(session),
                request.title(),
                request.dueDate());
    }

    @PatchMapping("/todos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateTodo(
            HttpSession session,
            @PathVariable long id,
            @Valid @RequestBody UpdateTodoRequest request) {
        if (!accountRepository.updateTodo(requireUserId(session), id, request.completed())) {
            throw new TodoNotFoundException();
        }
    }

    @DeleteMapping("/todos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTodo(HttpSession session, @PathVariable long id) {
        if (!accountRepository.deleteTodo(requireUserId(session), id)) {
            throw new TodoNotFoundException();
        }
    }

    @GetMapping("/todos.xlsx")
    public ResponseEntity<byte[]> exportTodos(HttpSession session) throws IOException {
        byte[] export = excelExportService.todos(accountRepository.findTodos(requireUserId(session)));
        return excelResponse("todo-list.xlsx", export);
    }

    private ResponseEntity<byte[]> excelResponse(String filename, byte[] data) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(data);
    }

    public record CreateTodoRequest(
            @NotBlank @Size(max = 255) String title,
            LocalDate dueDate) {
    }

    public record UpdateTodoRequest(@NotNull Boolean completed) {
    }
}
