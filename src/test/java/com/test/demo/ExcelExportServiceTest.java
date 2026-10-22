package com.test.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

class ExcelExportServiceTest {

    private final ExcelExportService exportService = new ExcelExportService();

    @Test
    void exportsLoginHistoryAsAnXlsxWorkbookWithUserAndResultColumns() throws Exception {
        byte[] file = exportService.loginHistory(
                List.of(new AccountRepository.LoginHistoryEntry(
                        1L,
                        LocalDateTime.of(2026, 10, 22, 9, 15),
                        false)),
                "Alex",
                "alex@example.com");

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(file))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("User");
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Email");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Alex");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("alex@example.com");
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("Failed");
        }
    }

    @Test
    void exportsTasksIncludingDatesAndCompletionState() throws Exception {
        byte[] file = exportService.todos(List.of(
                new AccountRepository.TodoItem(1L, "Study", LocalDate.of(2026, 10, 25), true)));

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(file))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Task");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Study");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("2026-10-25");
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("Completed");
        }
    }
}
