package md.uzina.server;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LeaveRequestService {

    /**
     * Submit a new leave request
     */
    public static int submitLeaveRequest(int userId, LocalDate dateFrom, LocalDate dateTo, String reason) throws SQLException {
        String sql = "INSERT INTO leave_requests (user_id, date_from, date_to, reason, status, created_at) " +
                "VALUES (?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP) " +
                "RETURNING id";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(dateFrom));
            stmt.setDate(3, Date.valueOf(dateTo));
            stmt.setString(4, reason);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return -1;
    }

    /**
     * Get all leave requests for a specific user
     */
    public static List<LeaveRequest> getUserLeaveRequests(int userId) throws SQLException {
        String sql = "SELECT id, user_id, date_from, date_to, reason, status, created_at " +
                "FROM leave_requests " +
                "WHERE user_id = ? " +
                "ORDER BY created_at DESC";
        
        List<LeaveRequest> requests = new ArrayList<>();
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LeaveRequest req = new LeaveRequest();
                    req.id = rs.getInt("id");
                    req.userId = rs.getInt("user_id");
                    req.dateFrom = rs.getDate("date_from").toLocalDate();
                    req.dateTo = rs.getDate("date_to").toLocalDate();
                    req.reason = rs.getString("reason");
                    req.status = rs.getString("status");
                    req.createdAt = rs.getTimestamp("created_at");
                    requests.add(req);
                }
            }
        }
        return requests;
    }

    /**
     * Get all leave requests for a team (all workers under a team leader)
     */
    public static List<LeaveRequestWithWorker> getTeamLeaveRequests(int teamLeaderId) throws SQLException {
        // Cererile echipei
        String sqlTeam = "SELECT lr.id, lr.user_id, u.name, lr.date_from, lr.date_to, lr.reason, lr.status, lr.created_at " +
                "FROM leave_requests lr " +
                "JOIN users u ON lr.user_id = u.id " +
                "JOIN team_assignments ta ON lr.user_id = ta.worker_id " +
                "WHERE ta.team_leader_id = ? ";
        // Cererile proprii
        String sqlOwn = "SELECT lr.id, lr.user_id, u.name, lr.date_from, lr.date_to, lr.reason, lr.status, lr.created_at " +
                "FROM leave_requests lr " +
                "JOIN users u ON lr.user_id = u.id " +
                "WHERE lr.user_id = ? ";

        List<LeaveRequestWithWorker> requests = new ArrayList<>();
        try (Connection conn = DBManager.getConnection()) {
            // Echipa
            try (PreparedStatement stmt = conn.prepareStatement(sqlTeam)) {
                stmt.setInt(1, teamLeaderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        LeaveRequestWithWorker req = new LeaveRequestWithWorker();
                        req.id = rs.getInt("id");
                        req.userId = rs.getInt("user_id");
                        req.workerName = rs.getString("name");
                        req.dateFrom = rs.getDate("date_from").toLocalDate();
                        req.dateTo = rs.getDate("date_to").toLocalDate();
                        req.reason = rs.getString("reason");
                        req.status = rs.getString("status");
                        req.createdAt = rs.getTimestamp("created_at");
                        requests.add(req);
                    }
                }
            }
            // Proprii
            try (PreparedStatement stmt = conn.prepareStatement(sqlOwn)) {
                stmt.setInt(1, teamLeaderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        LeaveRequestWithWorker req = new LeaveRequestWithWorker();
                        req.id = rs.getInt("id");
                        req.userId = rs.getInt("user_id");
                        req.workerName = rs.getString("name");
                        req.dateFrom = rs.getDate("date_from").toLocalDate();
                        req.dateTo = rs.getDate("date_to").toLocalDate();
                        req.reason = rs.getString("reason");
                        req.status = rs.getString("status");
                        req.createdAt = rs.getTimestamp("created_at");
                        requests.add(req);
                    }
                }
            }
        }
        // Sortare descrescătoare după created_at
        requests.sort((a, b) -> b.createdAt.compareTo(a.createdAt));
        return requests;
    }

    /**
     * Approve or reject a leave request (HR only)
     */
    public static boolean updateLeaveRequestStatus(int leaveRequestId, String newStatus) throws SQLException {
        String sql = "UPDATE leave_requests SET status = ? WHERE id = ?";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setInt(2, leaveRequestId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        }
    }

    /**
     * Get all leave requests (for HR to manage)
     */
    public static List<LeaveRequestWithWorker> getAllLeaveRequests() throws SQLException {
        String sql = "SELECT lr.id, lr.user_id, u.name, lr.date_from, lr.date_to, lr.reason, lr.status, lr.created_at " +
                "FROM leave_requests lr " +
                "JOIN users u ON lr.user_id = u.id " +
                "ORDER BY lr.created_at DESC";
        
        List<LeaveRequestWithWorker> requests = new ArrayList<>();
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LeaveRequestWithWorker req = new LeaveRequestWithWorker();
                    req.id = rs.getInt("id");
                    req.userId = rs.getInt("user_id");
                    req.workerName = rs.getString("name");
                    req.dateFrom = rs.getDate("date_from").toLocalDate();
                    req.dateTo = rs.getDate("date_to").toLocalDate();
                    req.reason = rs.getString("reason");
                    req.status = rs.getString("status");
                    req.createdAt = rs.getTimestamp("created_at");
                    requests.add(req);
                }
            }
        }
        return requests;
    }

    // Data classes
    public static class LeaveRequest {
        public int id;
        public int userId;
        public LocalDate dateFrom;
        public LocalDate dateTo;
        public String reason;
        public String status; // PENDING, APPROVED, REJECTED
        public java.sql.Timestamp createdAt;
    }

    public static class LeaveRequestWithWorker extends LeaveRequest {
        public String workerName;
    }
}
