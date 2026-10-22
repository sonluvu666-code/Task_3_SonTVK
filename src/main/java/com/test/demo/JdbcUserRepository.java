package com.test.demo;

import java.sql.Statement;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserRepository {

    private static final RowMapper<User> USER_ROW_MAPPER = (resultSet, rowNum) -> new User(
            resultSet.getLong("id"),
            resultSet.getString("name"),
            resultSet.getString("email"),
            resultSet.getString("password_hash"));

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByEmail(String email) {
        return jdbcTemplate.query(
                "SELECT id, name, email, password_hash FROM users WHERE email = ?",
                USER_ROW_MAPPER,
                email).stream().findFirst();
    }

    public Optional<User> findById(long id) {
        return jdbcTemplate.query(
                "SELECT id, name, email, password_hash FROM users WHERE id = ?",
                USER_ROW_MAPPER,
                id).stream().findFirst();
    }

    public void updatePassword(long id, String passwordHash) {
        int updated = jdbcTemplate.update(
                "UPDATE users SET password_hash = ? WHERE id = ?",
                passwordHash,
                id);
        if (updated != 1) {
            throw new IllegalStateException("The account no longer exists.");
        }
    }

    public Optional<User> findByName(String name) {
        return jdbcTemplate.query(
                "SELECT id, name, email, password_hash FROM users WHERE name = ?",
                USER_ROW_MAPPER,
                name).stream().findFirst();
    }

    public User insert(String name, String email, String passwordHash) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, name);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            return statement;
        }, keyHolder);

        Number id = keyHolder.getKey();
        if (id == null) {
            throw new IllegalStateException("The database did not return the generated user ID.");
        }
        return new User(id.longValue(), name, email, passwordHash);
    }
}
