package md.uzina.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LoginHistoryService {

    /**
     * Log a user login event
     */
    public static void logLogin(int userId, String userName, String userRole) throws SQLException {
        String sql = "INSERT INTO login_history (user_id, user_name, user_role, login_time, event_type) VALUES (?, ?, ?, CURRENT_TIMESTAMP, 'LOGIN')";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, userName);
            stmt.setString(3, userRole);
            stmt.executeUpdate();
        }
    }

    /**
     * Log a user logout event and compute session duration
     */
    public static void logLogout(int userId) throws SQLException {
        String sql = "SELECT id, login_time FROM login_history WHERE user_id = ? AND logout_time IS NULL ORDER BY login_time DESC LIMIT 1";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int logHistoryId = rs.getInt("id");
                    Timestamp loginTime = rs.getTimestamp("login_time");
                    Timestamp logoutTime = new Timestamp(System.currentTimeMillis());
                    long durationSeconds = (logoutTime.getTime() - loginTime.getTime()) / 1000;
                    
                    // Update the login_history row with logout time and duration
                    String updateSql = "UPDATE login_history SET logout_time = ?, session_duration_seconds = ?, event_type = 'LOGOUT' WHERE id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setTimestamp(1, logoutTime);
                        updateStmt.setInt(2, (int) durationSeconds);
                        updateStmt.setInt(3, logHistoryId);
                        updateStmt.executeUpdate();
                    }
                }
            }
        }
    }

    /**
     * Get login history for a date range (for HR audit trail)
     */
    public static List<LoginHistoryRecord> getLoginHistory(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT id, user_id, user_name, user_role, login_time, logout_time, session_duration_seconds, event_type " +
                "FROM login_history " +
                "WHERE DATE(login_time) BETWEEN ? AND ? " +
                "ORDER BY login_time DESC";
        
        List<LoginHistoryRecord> records = new ArrayList<>();
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(from));
            stmt.setDate(2, java.sql.Date.valueOf(to));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LoginHistoryRecord rec = new LoginHistoryRecord();
                    rec.id = rs.getInt("id");
                    rec.userId = rs.getInt("user_id");
                    rec.userName = rs.getString("user_name");
                    rec.userRole = rs.getString("user_role");
                    rec.loginTime = rs.getTimestamp("login_time");
                    rec.logoutTime = rs.getTimestamp("logout_time");
                    rec.sessionDurationSeconds = rs.getInt("session_duration_seconds");
                    rec.eventType = rs.getString("event_type");
                    records.add(rec);
                }
            }
        }
        return records;
    }

    /**
     * Get login history for a specific user
     */
    public static List<LoginHistoryRecord> getLoginHistoryForUser(int userId, LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT id, user_id, user_name, user_role, login_time, logout_time, session_duration_seconds, event_type " +
                "FROM login_history " +
                "WHERE user_id = ? AND DATE(login_time) BETWEEN ? AND ? " +
                "ORDER BY login_time DESC";
        
        List<LoginHistoryRecord> records = new ArrayList<>();
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setDate(2, java.sql.Date.valueOf(from));
            stmt.setDate(3, java.sql.Date.valueOf(to));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LoginHistoryRecord rec = new LoginHistoryRecord();
                    rec.id = rs.getInt("id");
                    rec.userId = rs.getInt("user_id");
                    rec.userName = rs.getString("user_name");
                    rec.userRole = rs.getString("user_role");
                    rec.loginTime = rs.getTimestamp("login_time");
                    rec.logoutTime = rs.getTimestamp("logout_time");
                    rec.sessionDurationSeconds = rs.getInt("session_duration_seconds");
                    rec.eventType = rs.getString("event_type");
                    records.add(rec);
                }
            }
        }
        return records;
    }

    // Data class
    public static class LoginHistoryRecord {
        public int id;
        public int userId;
        public String userName;
        public String userRole;
        public java.sql.Timestamp loginTime;
        public java.sql.Timestamp logoutTime;
        public int sessionDurationSeconds;
        public String eventType;
    }
}
