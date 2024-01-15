package com.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class EmployeeAnalysis {

    public static void main(String[] args) throws IOException {
        List<Employee> employees = parseExcel("Assignment_Timecard.xlsx");
        System.out.println("Successfully parsed");

        analyzeConsecutiveDays(employees);
        analyzeShiftGaps(employees);
        analyzeLongShifts(employees);
    }

    private static List<Employee> parseExcel(String filePath) throws IOException {
        List<Employee> employees = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (rowIterator.hasNext()) rowIterator.next(); // Skip header row

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Cell nameCell = row.getCell(7);
                Cell positionCell = row.getCell(0);
                Cell startTimeCell = row.getCell(2);
                Cell endTimeCell = row.getCell(3);

                String name = nameCell.getStringCellValue();
                String position = positionCell.getStringCellValue();
                LocalDateTime startTime = getLocalDateTimeFromCell(startTimeCell);
                LocalDateTime endTime = getLocalDateTimeFromCell(endTimeCell);

                if (startTime != null && endTime != null) {
                    employees.add(new Employee(name, position, startTime, endTime));
                }
            }
        }

        return employees;
    }

    private static LocalDateTime getLocalDateTimeFromCell(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;

        return (cell.getCellType() == CellType.NUMERIC)
                ? cell.getLocalDateTimeCellValue()
                : cell.getStringCellValue().trim().isEmpty()
                        ? null
                        : LocalDateTime.parse(cell.getStringCellValue(), DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a"));
    }

    private static void analyzeConsecutiveDays(List<Employee> employees) {
        for (int i = 0; i < employees.size() - 6; i++) {
            if (workedForConsecutiveDays(employees, i, i + 6) || workedForConsecutiveWorkingDays(employees, i, i + 6)) {
                System.out.println("Employee " + employees.get(i).getName() + " worked for 7 consecutive days.");
            }
        }
    }

    private static boolean workedForConsecutiveDays(List<Employee> employees, int start, int end) {
        for (int i = start; i < end; i++) {
            LocalDateTime currentEnd = employees.get(i).getEndTime();
            LocalDateTime nextStart = employees.get(i + 1).getStartTime();

            if (!currentEnd.plusDays(1).isEqual(nextStart)) return false;
        }
        return true;
    }

    private static boolean workedForConsecutiveWorkingDays(List<Employee> employees, int start, int end) {
        for (int i = start; i < end; i++) {
            LocalDateTime currentEnd = employees.get(i).getEndTime();
            LocalDateTime nextStart = employees.get(i + 1).getStartTime();

            long daysBetween = java.time.Duration.between(currentEnd.plusDays(1), nextStart).toDays();
            long weekendDays = countWeekendDays(currentEnd.plusDays(1), daysBetween);

            if (daysBetween - weekendDays != 1) return false;
        }
        return true;
    }

    private static long countWeekendDays(LocalDateTime startDate, long days) {
        long weekendDays = 0;
        for (int i = 0; i <= days; i++) {
            LocalDateTime currentDate = startDate.plusDays(i);
            if (currentDate.getDayOfWeek() == DayOfWeek.SATURDAY || currentDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                weekendDays++;
            }
        }
        return weekendDays;
    }

    private static void analyzeShiftGaps(List<Employee> employees) {
        for (int i = 0; i < employees.size() - 1; i++) {
            if (hasShiftGap(employees, i, i + 1)) {
                System.out.println("Employee " + employees.get(i).getName() +
                        " has less than 10 hours between shifts but greater than 1 hour.");
            }
        }
    }

    private static boolean hasShiftGap(List<Employee> employees, int firstIndex, int secondIndex) {
        Employee currentEmployee = employees.get(firstIndex);
        Employee nextEmployee = employees.get(secondIndex);

        long hoursBetween = java.time.Duration.between(currentEmployee.getEndTime(), nextEmployee.getStartTime()).toHours();

        return hoursBetween > 1 && hoursBetween < 10;
    }

    private static void analyzeLongShifts(List<Employee> employees) {
        for (Employee employee : employees) {
            if (employee != null && hasLongShift(employee)) {
                System.out.println("Employee " + employee.getName() + " worked for more than 14 hours in a single shift.");
            }
        }
    }

    private static boolean hasLongShift(Employee employee) {
        return java.time.Duration.between(employee.getStartTime(), employee.getEndTime()).toHours() > 14;
    }
}
