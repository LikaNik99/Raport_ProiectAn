package md.uzina.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class WorkHoursConfigService {
    
    /**
     * Get work hours configuration
     */
    public static WorkHoursConfig getConfig() throws SQLException {
        String sql = "SELECT start_hour, start_minute, end_hour, end_minute FROM work_hours_config ORDER BY id LIMIT 1";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new WorkHoursConfig(
                        rs.getInt("start_hour"),
                        rs.getInt("start_minute"),
                        rs.getInt("end_hour"),
                        rs.getInt("end_minute")
                    );
                }
            }
        }
        // Default: 8 AM - 5 PM
        return new WorkHoursConfig(8, 0, 17, 0);
    }
    
    /**
     * Update work hours configuration
     */
    public static void updateConfig(int startHour, int startMinute, int endHour, int endMinute) throws SQLException {
        String sql = "UPDATE work_hours_config SET start_hour = ?, start_minute = ?, end_hour = ?, end_minute = ?";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, startHour);
            stmt.setInt(2, startMinute);
            stmt.setInt(3, endHour);
            stmt.setInt(4, endMinute);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Check if today is a working day
     */
    public static boolean isWorkingDay() throws SQLException {
        int dayOfWeek = java.time.LocalDate.now().getDayOfWeek().getValue() % 7;  // 0=Sunday, 1=Monday, etc.
        String sql = "SELECT is_working_day FROM working_days WHERE day_of_week = ?";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, dayOfWeek);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("is_working_day");
                }
            }
        }
        return true;  // Default: working day
    }
    
    /**
     * Set working day status
     */
    public static void setWorkingDay(int dayOfWeek, boolean isWorkingDay) throws SQLException {
        String sql = "UPDATE working_days SET is_working_day = ? WHERE day_of_week = ?";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, isWorkingDay);
            stmt.setInt(2, dayOfWeek);
            stmt.executeUpdate();
        }
    }
    
    public static class WorkHoursConfig {
        public int startHour;
        public int startMinute;
        public int endHour;
        public int endMinute;
        
        public WorkHoursConfig(int startHour, int startMinute, int endHour, int endMinute) {
            this.startHour = startHour;
            this.startMinute = startMinute;
            this.endHour = endHour;
            this.endMinute = endMinute;
        }
    }
}
