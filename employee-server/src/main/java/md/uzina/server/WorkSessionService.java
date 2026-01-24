package md.uzina.server;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class WorkSessionService {
    
    /**
     * Start a work session for a user on the current date
     */
    public static void startWorkSession(int userId) throws SQLException {
        String sql = "INSERT INTO work_sessions (user_id, work_date, start_time, status) VALUES (?, ?, CURRENT_TIMESTAMP, 'ACTIVE') " +
                     "ON CONFLICT (user_id, work_date) DO UPDATE SET status = 'ACTIVE', start_time = CURRENT_TIMESTAMP";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(LocalDate.now()));
            stmt.executeUpdate();
        }
    }
    
    /**
     * End a work session for a user on the current date
     */
    public static void endWorkSession(int userId) throws SQLException {
        String sql = "UPDATE work_sessions SET end_time = CURRENT_TIMESTAMP, status = 'COMPLETED' " +
                     "WHERE user_id = ? AND work_date = CURRENT_DATE AND status = 'ACTIVE'";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Get current work status for a user
     */
    public static String getWorkStatus(int userId) throws SQLException {
        String sql = "SELECT status FROM work_sessions WHERE user_id = ? AND work_date = CURRENT_DATE";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        }
        return "NOT_STARTED";
    }
    
    /**
     * Automatically end all active work sessions (called at end of business hours)
     */
    public static void endAllActiveSessions() throws SQLException {
        String sql = "UPDATE work_sessions SET end_time = CURRENT_TIMESTAMP, status = 'COMPLETED' " +
                     "WHERE work_date = CURRENT_DATE AND status = 'ACTIVE'";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    /**
     * Get work status for all team members of a team leader
     */
    public static java.util.List<WorkStatusInfo> getTeamWorkStatus(int teamLeaderId) throws SQLException {
        java.util.List<WorkStatusInfo> result = new java.util.ArrayList<>();
        String sql = "SELECT u.id, u.name, u.role, ws.work_date, ws.start_time, ws.end_time, ws.status " +
                     "FROM users u " +
                     "INNER JOIN team_assignments ta ON u.id = ta.worker_id " +
                     "LEFT JOIN work_sessions ws ON u.id = ws.user_id AND ws.work_date = CURRENT_DATE " +
                     "WHERE ta.team_leader_id = ? " +
                     "ORDER BY u.name";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, teamLeaderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    WorkStatusInfo info = new WorkStatusInfo();
                    info.userId = rs.getInt("id");
                    info.userName = rs.getString("name");
                    info.userRole = rs.getString("role");
                    info.workDate = rs.getDate("work_date") != null ? rs.getDate("work_date").toString() : null;
                    info.startTime = rs.getTimestamp("start_time") != null ? rs.getTimestamp("start_time").toString() : null;
                    info.endTime = rs.getTimestamp("end_time") != null ? rs.getTimestamp("end_time").toString() : null;
                    info.status = rs.getString("status") != null ? rs.getString("status") : "NOT_STARTED";
                    result.add(info);
                }
            }
        }
        return result;
    }

    /**
     * Get work status for all workers and team leaders (for HR and Admin)
     * @param mode "current" - today only (default), "full_history" - all dates, "last_per_employee" - last start for each employee
     */
    public static java.util.List<WorkStatusInfo> getAllWorkStatus(String mode) throws SQLException {
        java.util.List<WorkStatusInfo> result = new java.util.ArrayList<>();
        String sql;
        
        if ("full_history".equals(mode)) {
            // All work sessions for all employees, ordered by most recent first
            sql = "SELECT u.id, u.name, u.role, ws.work_date, ws.start_time, ws.end_time, ws.status " +
                  "FROM users u " +
                  "INNER JOIN work_sessions ws ON u.id = ws.user_id " +
                  "WHERE u.role IN ('WORKER', 'TEAMLEADER') " +
                  "ORDER BY ws.work_date DESC, ws.start_time DESC";
        } else if ("last_per_employee".equals(mode)) {
            // Last start_time work session for each employee
            sql = "SELECT u.id, u.name, u.role, ws.work_date, ws.start_time, ws.end_time, ws.status " +
                  "FROM users u " +
                  "INNER JOIN work_sessions ws ON u.id = ws.user_id " +
                  "WHERE u.role IN ('WORKER', 'TEAMLEADER') " +
                  "AND (ws.user_id, ws.start_time) IN ( " +
                  "    SELECT user_id, MAX(start_time) " +
                  "    FROM work_sessions " +
                  "    GROUP BY user_id " +
                  ") " +
                  "ORDER BY ws.start_time DESC";
        } else {
            // Default: current date only
            sql = "SELECT u.id, u.name, u.role, ws.work_date, ws.start_time, ws.end_time, ws.status " +
                  "FROM users u " +
                  "LEFT JOIN work_sessions ws ON u.id = ws.user_id AND ws.work_date = CURRENT_DATE " +
                  "WHERE u.role IN ('WORKER', 'TEAMLEADER') " +
                  "ORDER BY u.role, u.name";
        }
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    WorkStatusInfo info = new WorkStatusInfo();
                    info.userId = rs.getInt("id");
                    info.userName = rs.getString("name");
                    info.userRole = rs.getString("role");
                    info.workDate = rs.getDate("work_date") != null ? rs.getDate("work_date").toString() : null;
                    info.startTime = rs.getTimestamp("start_time") != null ? rs.getTimestamp("start_time").toString() : null;
                    info.endTime = rs.getTimestamp("end_time") != null ? rs.getTimestamp("end_time").toString() : null;
                    info.status = rs.getString("status") != null ? rs.getString("status") : "NOT_STARTED";
                    result.add(info);
                }
            }
        }
        return result;
    }
    
    /**
     * Overloaded method for backward compatibility - defaults to "current" mode
     */
    public static java.util.List<WorkStatusInfo> getAllWorkStatus() throws SQLException {
        return getAllWorkStatus("current");
    }

    /**
     * Inner class to hold work status information
     */
    public static class WorkStatusInfo {
        public int userId;
        public String userName;
        public String userRole;
        public String workDate;
        public String startTime;
        public String endTime;
        public String status;
    }
}
