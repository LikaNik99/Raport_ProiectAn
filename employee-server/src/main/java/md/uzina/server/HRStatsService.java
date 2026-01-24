package md.uzina.server;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HRStatsService {

    /**
     * Get daily statistics for all workers on a given date range
     * Returns: date, total_users, present_count, leave_count, absent_excused, absent_unexcused
     */
    public static List<DailyStats> getDailyStats(LocalDate from, LocalDate to) throws SQLException {
        List<DailyStats> stats = new ArrayList<>();
        int totalUsers = getTotalActiveUsers();
        // iterate each date in the range so HR sees days even without sessions
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            DailyStats ds = new DailyStats();
            ds.date = d;
            ds.totalUsers = totalUsers;
            ds.presentCount = getPresentCount(d);
            ds.leaveCount = getLeaveCount(d);
            int absent = Math.max(0, ds.totalUsers - ds.presentCount - ds.leaveCount);
            ds.absentExcused = ds.leaveCount; // treated as excused
            ds.absentUnexcused = absent;
            stats.add(ds);
        }
        return stats;
    }

    /**
     * Get user-level statistics for a given date range (per user attendance)
     */
    public static List<UserStats> getUserStats(LocalDate from, LocalDate to, String userFilter) throws SQLException {
        String sql = "SELECT u.id, u.name, " +
                "COUNT(DISTINCT CASE WHEN ws.status = 'COMPLETED' THEN ws.work_date END) as days_worked, " +
                "COUNT(DISTINCT CASE WHEN lr.status = 'APPROVED' THEN lr.date_from END) as days_leave, " +
                "COUNT(DISTINCT CASE WHEN (ws.user_id IS NULL AND lr.id IS NULL) THEN ws.work_date END) as days_absent " +
                "FROM users u " +
                "LEFT JOIN work_sessions ws ON u.id = ws.user_id AND ws.work_date BETWEEN ? AND ? " +
                "LEFT JOIN leave_requests lr ON u.id = lr.user_id AND lr.date_from BETWEEN ? AND ? AND lr.status = 'APPROVED' " +
                "WHERE u.role IN ('WORKER', 'TEAMLEADER') " +
                (userFilter != null && !userFilter.isEmpty() ? "AND (u.name ILIKE ? OR CAST(u.id AS VARCHAR) LIKE ?) " : "") +
                "GROUP BY u.id, u.name " +
                "ORDER BY u.name";
        
        List<UserStats> stats = new ArrayList<>();
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int idx = 1;
            stmt.setDate(idx++, Date.valueOf(from));
            stmt.setDate(idx++, Date.valueOf(to));
            stmt.setDate(idx++, Date.valueOf(from));
            stmt.setDate(idx++, Date.valueOf(to));
            if (userFilter != null && !userFilter.isEmpty()) {
                stmt.setString(idx++, "%" + userFilter + "%");
                stmt.setString(idx++, "%" + userFilter + "%");
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UserStats us = new UserStats();
                    us.userId = rs.getInt("id");
                    us.userName = rs.getString("name");
                    us.daysWorked = rs.getInt("days_worked");
                    us.daysLeave = rs.getInt("days_leave");
                    us.daysAbsent = rs.getInt("days_absent");
                    stats.add(us);
                }
            }
        }
        return stats;
    }

    /**
     * Get all work sessions for export (full audit trail)
     */
    public static List<WorkSessionRecord> getAllWorkSessions(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT u.id, u.name, u.role, ws.work_date, ws.start_time, ws.end_time, ws.status " +
                "FROM work_sessions ws " +
                "JOIN users u ON ws.user_id = u.id " +
                "WHERE ws.work_date BETWEEN ? AND ? " +
                "ORDER BY ws.work_date DESC, u.name ASC";
        
        List<WorkSessionRecord> records = new ArrayList<>();
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(from));
            stmt.setDate(2, Date.valueOf(to));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    WorkSessionRecord rec = new WorkSessionRecord();
                    rec.userId = rs.getInt("id");
                    rec.userName = rs.getString("name");
                    rec.userRole = rs.getString("role");
                    rec.workDate = rs.getDate("work_date").toLocalDate();
                    rec.startTime = rs.getTimestamp("start_time");
                    rec.endTime = rs.getTimestamp("end_time");
                    rec.status = rs.getString("status");
                    records.add(rec);
                }
            }
        }
        return records;
    }

    /**
     * Get all leave requests with their status
     */
    public static List<LeaveRequestRecord> getAllLeaveRequests(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT lr.id, u.id as user_id, u.name, lr.date_from, lr.date_to, lr.reason, lr.status " +
                "FROM leave_requests lr " +
                "JOIN users u ON lr.user_id = u.id " +
                "WHERE lr.date_from BETWEEN ? AND ? OR lr.date_to BETWEEN ? AND ? " +
                "ORDER BY lr.date_from DESC";
        
        List<LeaveRequestRecord> records = new ArrayList<>();
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(from));
            stmt.setDate(2, Date.valueOf(to));
            stmt.setDate(3, Date.valueOf(from));
            stmt.setDate(4, Date.valueOf(to));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LeaveRequestRecord rec = new LeaveRequestRecord();
                    rec.id = rs.getInt("id");
                    rec.userId = rs.getInt("user_id");
                    rec.userName = rs.getString("name");
                    rec.dateFrom = rs.getDate("date_from").toLocalDate();
                    rec.dateTo = rs.getDate("date_to").toLocalDate();
                    rec.reason = rs.getString("reason");
                    rec.status = rs.getString("status");
                    records.add(rec);
                }
            }
        }
        return records;
    }

    // Helper methods
    private static int getTotalActiveUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role IN ('WORKER', 'TEAMLEADER')";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private static int getPresentCount(LocalDate date) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT user_id) FROM work_sessions WHERE work_date = ? AND status = 'COMPLETED'";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private static int getLeaveCount(LocalDate date) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT user_id) FROM leave_requests " +
                "WHERE (date_from <= ? AND date_to >= ?) AND status = 'APPROVED'";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private static int getAbsentExcused(LocalDate date) throws SQLException {
        // Deprecated helper - leave for compatibility but compute excused via leave requests
        return getLeaveCount(date);
    }

    private static int getAbsentUnexcused(LocalDate date) throws SQLException {
        int total = getTotalActiveUsers();
        int present = getPresentCount(date);
        int leave = getLeaveCount(date);
        int absent = Math.max(0, total - present - leave);
        return absent;
    }

    // Data classes
    public static class DailyStats {
        public LocalDate date;
        public int totalUsers;
        public int presentCount;
        public int leaveCount;
        public int absentExcused;
        public int absentUnexcused;
    }

    public static class UserStats {
        public int userId;
        public String userName;
        public int daysWorked;
        public int daysLeave;
        public int daysAbsent;
    }

    public static class WorkSessionRecord {
        public int userId;
        public String userName;
        public String userRole;
        public LocalDate workDate;
        public java.sql.Timestamp startTime;
        public java.sql.Timestamp endTime;
        public String status;
    }

    public static class LeaveRequestRecord {
        public int id;
        public int userId;
        public String userName;
        public LocalDate dateFrom;
        public LocalDate dateTo;
        public String reason;
        public String status;
    }
}
