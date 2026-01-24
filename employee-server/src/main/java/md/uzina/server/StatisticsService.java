package md.uzina.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class StatisticsService {

    /**
     * Calculate monthly statistics for all employees
     * Shows working days, days worked, paid leaves, absences, and attendance rate
     */
    public static JsonArray getMonthlyStatistics(int month, int year) throws SQLException {
        JsonArray result = new JsonArray();
        
        // Calculate working days (Monday-Friday) for the month
        int workingDays = calculateWorkingDays(year, month);
        
        // Get work hours config
        double dailyHours = getConfiguredDailyHours();
        double expectedHoursPerMonth = workingDays * dailyHours;
        
        String sql = "SELECT id, name, role FROM users WHERE role IN ('WORKER', 'TEAMLEADER') ORDER BY name";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                int userId = rs.getInt("id");
                String name = rs.getString("name");
                String role = rs.getString("role");
                
                JsonObject empStats = new JsonObject();
                empStats.addProperty("user_id", userId);
                empStats.addProperty("name", name);
                empStats.addProperty("role", role);
                empStats.addProperty("working_days", workingDays);
                
                // Calculate days worked (distinct dates with work sessions)
                int daysWorked = calculateDaysWorked(userId, year, month);
                empStats.addProperty("days_worked", daysWorked);
                
                // Calculate paid leaves (HR approved only)
                int paidLeaves = calculatePaidLeaves(userId, year, month);
                empStats.addProperty("paid_leaves", paidLeaves);
                
                // Calculate absent days
                int daysAbsent = workingDays - daysWorked - paidLeaves;
                if (daysAbsent < 0) daysAbsent = 0;
                empStats.addProperty("days_absent", daysAbsent);
                
                // Calculate actual hours worked
                double actualHours = calculateActualHours(userId, year, month);
                empStats.addProperty("actual_hours", actualHours);
                empStats.addProperty("expected_hours", expectedHoursPerMonth);
                
                // Calculate attendance rate (%)
                double attendanceRate = 0.0;
                if (expectedHoursPerMonth > 0) {
                    attendanceRate = (actualHours / expectedHoursPerMonth) * 100.0;
                }
                empStats.addProperty("attendance_rate", attendanceRate);
                
                result.add(empStats);
            }
        }
        
        return result;
    }

    /**
     * Calculate working days (Monday-Friday) in a given month
     */
    private static int calculateWorkingDays(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        
        int workingDays = 0;
        LocalDate current = start;
        
        while (!current.isAfter(end)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        
        return workingDays;
    }

    /**
     * Get configured daily working hours
     */
    private static double getConfiguredDailyHours() {
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT start_hour, start_minute, end_hour, end_minute FROM work_hours_config WHERE id = 1")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int startHour = rs.getInt("start_hour");
                int startMinute = rs.getInt("start_minute");
                int endHour = rs.getInt("end_hour");
                int endMinute = rs.getInt("end_minute");
                
                // Calculate total hours
                double startTime = startHour + (startMinute / 60.0);
                double endTime = endHour + (endMinute / 60.0);
                
                return endTime - startTime;
            }
        } catch (Exception e) {
            System.err.println("Error getting work hours config: " + e.getMessage());
        }
        return 8.0; // Default 8 hours
    }

    /**
     * Calculate number of distinct days worked in a month
     */
    private static int calculateDaysWorked(int userId, int year, int month) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT work_date) as days_count " +
                     "FROM work_sessions " +
                     "WHERE user_id = ? " +
                     "AND EXTRACT(YEAR FROM work_date) = ? " +
                     "AND EXTRACT(MONTH FROM work_date) = ?";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, year);
            stmt.setInt(3, month);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("days_count");
            }
        }
        return 0;
    }

    /**
     * Calculate number of paid leave days (HR approved only) that fall on working days
     */
    private static int calculatePaidLeaves(int userId, int year, int month) throws SQLException {
        String sql = "SELECT date_from, date_to " +
                     "FROM leave_requests " +
                     "WHERE user_id = ? " +
                     "AND status = 'APPROVED' " +
                     "AND ((EXTRACT(YEAR FROM date_from) = ? AND EXTRACT(MONTH FROM date_from) = ?) " +
                     "     OR (EXTRACT(YEAR FROM date_to) = ? AND EXTRACT(MONTH FROM date_to) = ?)" +
                     "     OR (date_from < ? AND date_to > ?))";
        
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        
        int totalPaidDays = 0;
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, year);
            stmt.setInt(3, month);
            stmt.setInt(4, year);
            stmt.setInt(5, month);
            stmt.setDate(6, java.sql.Date.valueOf(monthStart));
            stmt.setDate(7, java.sql.Date.valueOf(monthEnd));
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                LocalDate leaveStart = rs.getDate("date_from").toLocalDate();
                LocalDate leaveEnd = rs.getDate("date_to").toLocalDate();
                
                // Constrain to current month
                if (leaveStart.isBefore(monthStart)) leaveStart = monthStart;
                if (leaveEnd.isAfter(monthEnd)) leaveEnd = monthEnd;
                
                // Count working days in this range
                LocalDate current = leaveStart;
                while (!current.isAfter(leaveEnd)) {
                    DayOfWeek dayOfWeek = current.getDayOfWeek();
                    if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                        totalPaidDays++;
                    }
                    current = current.plusDays(1);
                }
            }
        }
        
        return totalPaidDays;
    }

    /**
     * Calculate actual hours worked in a month
     */
    private static double calculateActualHours(int userId, int year, int month) throws SQLException {
        String sql = "SELECT start_time, end_time " +
                     "FROM work_sessions " +
                     "WHERE user_id = ? " +
                     "AND EXTRACT(YEAR FROM work_date) = ? " +
                     "AND EXTRACT(MONTH FROM work_date) = ? " +
                     "AND end_time IS NOT NULL";
        
        double totalMinutes = 0.0;
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, year);
            stmt.setInt(3, month);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                java.sql.Timestamp startTime = rs.getTimestamp("start_time");
                java.sql.Timestamp endTime = rs.getTimestamp("end_time");
                
                if (startTime != null && endTime != null) {
                    long minutes = ChronoUnit.MINUTES.between(
                        startTime.toLocalDateTime(),
                        endTime.toLocalDateTime()
                    );
                    totalMinutes += minutes;
                }
            }
        }
        
        return totalMinutes / 60.0; // Convert to hours
    }
}
