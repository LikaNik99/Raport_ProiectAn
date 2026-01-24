package md.uzina.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminService {
    private static final Logger LOG = LoggerFactory.getLogger(AdminService.class);

    /**
     * Bulk import users from a list of EmployeeData objects
     * @param employees List of employee data (id, name, password, role)
     * @return Number of successfully inserted rows
     */
    public static int importEmployees(List<EmployeeData> employees) {
        int count = 0;
        String sql = "INSERT INTO users (id, name, role, password_hash) VALUES (?, ?, ?, ?)";
        try (Connection c = DBManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (EmployeeData emp : employees) {
                try {
                    ps.setInt(1, emp.getId());
                    ps.setString(2, emp.getName());
                    ps.setString(3, emp.getRole());
                    ps.setString(4, emp.getPassword());
                    ps.addBatch();
                } catch (Exception e) {
                    LOG.warn("importEmployees: skipping invalid employee {}", emp.getId(), e);
                }
            }
            int[] results = ps.executeBatch();
            count = results.length;
            LOG.info("importEmployees: imported {} employees", count);
        } catch (SQLException e) {
            LOG.error("importEmployees: database error", e);
        }
        return count;
    }

    /**
     * Get all workers (WORKER role)
     */
    public static List<Worker> getAllWorkers() throws SQLException {
        String sql = "SELECT u.id, u.name, u.role, u.phone, u.address, u.job, u.team_leader_id, " +
                     "tl.name as team_leader_name " +
                     "FROM users u " +
                     "LEFT JOIN users tl ON u.team_leader_id = tl.id " +
                     "ORDER BY u.name";
        
        List<Worker> workers = new ArrayList<>();
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Worker w = new Worker();
                    w.id = rs.getInt("id");
                    w.name = rs.getString("name");
                    w.role = rs.getString("role");
                    w.phone = rs.getString("phone");
                    w.address = rs.getString("address");
                    w.job = rs.getString("job");
                    w.teamLeaderId = rs.getObject("team_leader_id", Integer.class);
                    w.teamLeaderName = rs.getString("team_leader_name");
                    workers.add(w);
                }
            }
        }
        return workers;
    }

    /**
     * Change a worker to team leader (update role in users table and remove from team_assignments)
     */
    public static boolean changeWorkerToTeamLeader(int workerId) throws SQLException {
        // First remove from team_assignments (as a worker can't have a team leader once promoted)
        String deleteSql = "DELETE FROM team_assignments WHERE worker_id = ?";
        // Then update the role
        String updateSql = "UPDATE users SET role = 'TEAMLEADER' WHERE id = ? AND role = 'WORKER'";
        
        try (Connection conn = DBManager.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                stmt.setInt(1, workerId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setInt(1, workerId);
                int rows = stmt.executeUpdate();
                return rows > 0;
            }
        }
    }

    /**
     * Change a team leader back to worker
     */
    public static boolean changeTeamLeaderToWorker(int teamLeaderId) throws SQLException {
        // First delete team assignments
        String deleteSql = "DELETE FROM team_assignments WHERE team_leader_id = ?";
        // Then update the role
        String updateSql = "UPDATE users SET role = 'WORKER' WHERE id = ? AND role = 'TEAMLEADER'";
        
        try (Connection conn = DBManager.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                stmt.setInt(1, teamLeaderId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setInt(1, teamLeaderId);
                int rows = stmt.executeUpdate();
                return rows > 0;
            }
        }
    }

    /**
     * Assign a worker to a team leader
     */
    public static boolean assignWorkerToTeamLeader(int workerId, int teamLeaderId) throws SQLException {
        String sql = "INSERT INTO team_assignments (team_leader_id, worker_id, assigned_date) " +
                "VALUES (?, ?, CURRENT_DATE) " +
                "ON CONFLICT (team_leader_id, worker_id) DO NOTHING";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, teamLeaderId);
            stmt.setInt(2, workerId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        }
    }

    /**
     * Remove worker from team leader
     */
    public static boolean removeWorkerFromTeamLeader(int workerId, int teamLeaderId) throws SQLException {
        String sql = "DELETE FROM team_assignments WHERE worker_id = ? AND team_leader_id = ?";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, workerId);
            stmt.setInt(2, teamLeaderId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        }
    }

    /**
     * Get team leaders only
     */
    public static List<Worker> getAllTeamLeaders() throws SQLException {
        String sql = "SELECT u.id, u.name, u.role FROM users u WHERE u.role = 'TEAMLEADER' ORDER BY u.name";
        
        List<Worker> leaders = new ArrayList<>();
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Worker w = new Worker();
                    w.id = rs.getInt("id");
                    w.name = rs.getString("name");
                    w.role = rs.getString("role");
                    leaders.add(w);
                }
            }
        }
        return leaders;
    }

    /**
     * Get team leader for a specific worker
     */
    public static Integer getTeamLeaderForWorker(int workerId) throws SQLException {
        String sql = "SELECT team_leader_id FROM team_assignments WHERE worker_id = ? ORDER BY assigned_date DESC LIMIT 1";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, workerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("team_leader_id");
                }
            }
        }
        return null;
    }

    /**
     * Get all workers with their team leader info (if assigned)
     */
    public static List<WorkerWithTeamLeader> getAllWorkersWithTeamLeader() throws SQLException {
        String sql = "SELECT u.id, u.name, u.role, " +
                "ta.team_leader_id, tl.name as team_leader_name " +
                "FROM users u " +
                "LEFT JOIN team_assignments ta ON u.id = ta.worker_id " +
                "LEFT JOIN users tl ON ta.team_leader_id = tl.id " +
                "WHERE u.role IN ('WORKER', 'TEAMLEADER') " +
                "ORDER BY u.role DESC, u.name ASC";
        
        List<WorkerWithTeamLeader> workers = new ArrayList<>();
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    WorkerWithTeamLeader w = new WorkerWithTeamLeader();
                    w.id = rs.getInt("id");
                    w.name = rs.getString("name");
                    w.role = rs.getString("role");
                    w.teamLeaderId = rs.getObject("team_leader_id") != null ? rs.getInt("team_leader_id") : null;
                    w.teamLeaderName = rs.getString("team_leader_name");
                    workers.add(w);
                }
            }
        }
        return workers;
    }

    /**
     * Simple Worker wrapper class
     */
    public static class Worker {
        public int id;
        public String name;
        public String role;
        public String phone;
        public String address;
        public String job;
        public Integer teamLeaderId;
        public String teamLeaderName;
    }

    /**
     * Simple EmployeeData wrapper class
     */
    public static class EmployeeData {
        private int id;
        private String name;
        private String role;
        private String password;

        public EmployeeData(int id, String name, String role, String password) {
            this.id = id;
            this.name = name;
            this.role = role;
            this.password = password;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getRole() { return role; }
        public String getPassword() { return password; }
    }

    /**
     * Worker with Team Leader info
     */
    public static class WorkerWithTeamLeader {
        public int id;
        public String name;
        public String role;
        public Integer teamLeaderId;
        public String teamLeaderName;
    }

    /**
     * Update user password
     */
    public static boolean updateUserPassword(int userId, String newPassword) throws SQLException {
        // For now, store password as-is (same as authentication method)
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPassword);
            stmt.setInt(2, userId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        }
    }

    /**
     * Create a new user and return generated id, or -1 on failure
     */
    public static int createUser(String name, String role, String password, String phone, String address) throws SQLException {
        String sql = "INSERT INTO users (name, role, password_hash, phone, address) VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, role != null && !role.isEmpty() ? role : "WORKER");
            stmt.setString(3, password);
            stmt.setString(4, phone);
            stmt.setString(5, address);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return -1;
    }

    /**
     * Update user info (name and optionally role)
     */
    public static boolean updateUserInfo(int userId, String newName, String newRole, String newPhone, String newAddress, String newJob, Integer teamLeaderId) throws SQLException {
        String sql = "UPDATE users SET name = ?, role = ?, phone = ?, address = ?, job = ?, team_leader_id = ? WHERE id = ?";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newName);
            stmt.setString(2, newRole != null && !newRole.isEmpty() ? newRole : "WORKER");
            stmt.setString(3, newPhone != null && !newPhone.isEmpty() ? newPhone : null);
            stmt.setString(4, newAddress != null && !newAddress.isEmpty() ? newAddress : null);
            stmt.setString(5, newJob != null && !newJob.isEmpty() ? newJob : null);
            if (teamLeaderId != null) {
                stmt.setInt(6, teamLeaderId);
            } else {
                stmt.setNull(6, java.sql.Types.INTEGER);
            }
            stmt.setInt(7, userId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        }
    }

    /**
     * Create a new user with full details for import (overload with is_worker and team_leader_id)
     */
    public static int createUser(String name, String password, String job, String phone, String address, 
                                   boolean isWorker, Integer teamLeaderId) throws SQLException {
        String role = isWorker ? "WORKER" : "TEAM_LEADER";
        String sql = "INSERT INTO users (name, role, password_hash, job, phone, address, team_leader_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, role);
            stmt.setString(3, password);
            stmt.setString(4, job);
            stmt.setString(5, phone);
            stmt.setString(6, address);
            if (teamLeaderId != null) {
                stmt.setInt(7, teamLeaderId);
            } else {
                stmt.setNull(7, java.sql.Types.INTEGER);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return -1;
    }

    /**
     * Get all job titles
     */
    public static List<JobTitle> getJobTitles() throws SQLException {
        String sql = "SELECT id, title, description FROM job_titles ORDER BY title";
        
        List<JobTitle> jobTitles = new ArrayList<>();
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JobTitle jt = new JobTitle();
                    jt.id = rs.getInt("id");
                    jt.title = rs.getString("title");
                    jt.description = rs.getString("description");
                    jobTitles.add(jt);
                }
            }
        }
        return jobTitles;
    }

    /**
     * Delete a user by ID
     */
    public static boolean deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        }
    }

    /**
     * Import a work session (for bulk import from Excel)
     */
    public static void importWorkSession(int userId, String startTime, String endTime) throws SQLException {
        String sql;
        String status;
        
        if (endTime != null && !endTime.isEmpty()) {
            // Dacă există end_time, sesiunea este completă
            status = "COMPLETED";
            sql = "INSERT INTO work_sessions (user_id, work_date, start_time, end_time, status) " +
                  "VALUES (?, ?::timestamp::date, ?::timestamp, ?::timestamp, ?)";
        } else {
            // Dacă nu există end_time, sesiunea este activă
            status = "ACTIVE";
            sql = "INSERT INTO work_sessions (user_id, work_date, start_time, status) " +
                  "VALUES (?, ?::timestamp::date, ?::timestamp, ?)";
        }
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, startTime); // work_date extracted from start_time
            stmt.setString(3, startTime);
            if (endTime != null && !endTime.isEmpty()) {
                stmt.setString(4, endTime);
                stmt.setString(5, status);
            } else {
                stmt.setString(4, status);
            }
            stmt.executeUpdate();
        }
    }

    // Data classes
    public static class JobTitle {
        public int id;
        public String title;
        public String description;
    }
}
