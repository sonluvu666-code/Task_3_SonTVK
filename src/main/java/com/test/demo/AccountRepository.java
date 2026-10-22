package com.test.demo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Date;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {

    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 4096;

    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void recordLoginAttempt(long userId, boolean successful) {
        jdbcTemplate.update(
                "INSERT INTO login_history (user_id, successful) VALUES (?, ?)",
                userId,
                successful);
    }

    public List<LoginHistoryEntry> findLoginHistory(long userId) {
        return jdbcTemplate.query(
                """
                        SELECT id, login_time, successful
                        FROM login_history
                        WHERE user_id = ?
                        ORDER BY login_time DESC, id DESC
                        """,
                (resultSet, rowNum) -> new LoginHistoryEntry(
                        resultSet.getLong("id"),
                        resultSet.getTimestamp("login_time").toLocalDateTime(),
                        resultSet.getBoolean("successful")),
                userId);
    }

    public Optional<ProfileImage> findProfileImage(long userId) {
        return jdbcTemplate.query(
                "SELECT content_type, image_data FROM profile_images WHERE user_id = ?",
                (resultSet, rowNum) -> new ProfileImage(
                        resultSet.getString("content_type"),
                        resultSet.getBytes("image_data")),
                userId).stream().findFirst();
    }

    public void storeProfileImage(long userId, byte[] data, String declaredContentType) {
        String contentType = validateImage(data, declaredContentType);
        jdbcTemplate.update(
                """
                        INSERT INTO profile_images (user_id, content_type, image_data)
                        VALUES (?, ?, ?)
                        ON DUPLICATE KEY UPDATE content_type = VALUES(content_type),
                                                image_data = VALUES(image_data)
                        """,
                userId,
                contentType,
                data);
    }

    public List<TodoItem> findTodos(long userId) {
        return jdbcTemplate.query(
                """
                        SELECT id, title, due_date, completed
                        FROM todo_items
                        WHERE user_id = ?
                        ORDER BY completed ASC, due_date IS NULL ASC, due_date ASC, created_at DESC
                        """,
                (resultSet, rowNum) -> new TodoItem(
                        resultSet.getLong("id"),
                        resultSet.getString("title"),
                        resultSet.getDate("due_date") == null
                                ? null
                                : resultSet.getDate("due_date").toLocalDate(),
                        resultSet.getBoolean("completed")),
                userId);
    }

    public TodoItem addTodo(long userId, String title, LocalDate dueDate) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO todo_items (user_id, title, due_date) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, userId);
            statement.setString(2, title.trim());
            if (dueDate == null) {
                statement.setNull(3, java.sql.Types.DATE);
            } else {
                statement.setDate(3, Date.valueOf(dueDate));
            }
            return statement;
        }, keyHolder);
        Number id = keyHolder.getKey();
        if (id == null) {
            throw new IllegalStateException("The database did not return the generated task ID.");
        }
        return new TodoItem(id.longValue(), title.trim(), dueDate, false);
    }

    public boolean updateTodo(long userId, long todoId, boolean completed) {
        return jdbcTemplate.update(
                "UPDATE todo_items SET completed = ? WHERE id = ? AND user_id = ?",
                completed,
                todoId,
                userId) == 1;
    }

    public boolean deleteTodo(long userId, long todoId) {
        return jdbcTemplate.update(
                "DELETE FROM todo_items WHERE id = ? AND user_id = ?",
                todoId,
                userId) == 1;
    }

    private String validateImage(byte[] data, String declaredContentType) {
        if (data == null || data.length == 0 || data.length > MAX_IMAGE_BYTES) {
            throw new InvalidImageException();
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (input == null) {
                throw new InvalidImageException();
            }
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new InvalidImageException();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                String contentType = switch (format) {
                    case "png" -> "image/png";
                    case "jpeg", "jpg" -> "image/jpeg";
                    case "gif" -> "image/gif";
                    default -> throw new InvalidImageException();
                };
                if (width < 1 || height < 1 || width > MAX_IMAGE_DIMENSION
                        || height > MAX_IMAGE_DIMENSION || !contentType.equals(declaredContentType)) {
                    throw new InvalidImageException();
                }
                return contentType;
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new InvalidImageException();
        }
    }

    public record LoginHistoryEntry(long id, LocalDateTime loginTime, boolean successful) {
    }

    public record ProfileImage(String contentType, byte[] data) {
    }

    public record TodoItem(long id, String title, LocalDate dueDate, boolean completed) {
    }
}
