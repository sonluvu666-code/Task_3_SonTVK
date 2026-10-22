package com.test.demo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExcelExportService {

    private static final DateTimeFormatter LOGIN_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] loginHistory(
            List<AccountRepository.LoginHistoryEntry> entries,
            String userName,
            String email) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Login history");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("User");
            header.createCell(1).setCellValue("Email");
            header.createCell(2).setCellValue("Login time");
            header.createCell(3).setCellValue("Result");
            for (int index = 0; index < entries.size(); index++) {
                var entry = entries.get(index);
                Row row = sheet.createRow(index + 1);
                row.createCell(0).setCellValue(userName);
                row.createCell(1).setCellValue(email);
                row.createCell(2).setCellValue(LOGIN_TIME_FORMAT.format(entry.loginTime()));
                row.createCell(3).setCellValue(entry.successful() ? "Success" : "Failed");
            }
            sheet.setColumnWidth(0, 20 * 256);
            sheet.setColumnWidth(1, 32 * 256);
            sheet.setColumnWidth(2, 24 * 256);
            sheet.setColumnWidth(3, 16 * 256);
            return write(workbook);
        }
    }

    public byte[] todos(List<AccountRepository.TodoItem> todos) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("To-do list");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Task");
            header.createCell(1).setCellValue("Due date");
            header.createCell(2).setCellValue("Status");
            for (int index = 0; index < todos.size(); index++) {
                var todo = todos.get(index);
                Row row = sheet.createRow(index + 1);
                row.createCell(0).setCellValue(todo.title());
                row.createCell(1).setCellValue(todo.dueDate() == null ? "" : todo.dueDate().toString());
                row.createCell(2).setCellValue(todo.completed() ? "Completed" : "Pending");
            }
            sheet.setColumnWidth(0, 48 * 256);
            sheet.setColumnWidth(1, 16 * 256);
            sheet.setColumnWidth(2, 16 * 256);
            return write(workbook);
        }
    }

    private byte[] write(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
