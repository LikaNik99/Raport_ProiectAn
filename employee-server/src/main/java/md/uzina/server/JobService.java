package md.uzina.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class JobService {
    
    public static List<JobTitle> getAllJobs() throws SQLException {
        String sql = "SELECT id, title, description, created_at FROM job_titles ORDER BY title";
        List<JobTitle> jobs = new ArrayList<>();
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String description = rs.getString("description");
                Timestamp ts = rs.getTimestamp("created_at");
                
                JobTitle job = new JobTitle(
                    id,
                    title,
                    description,
                    ts != null ? ts.toLocalDateTime() : null
                );
                jobs.add(job);
            }
        }
        
        return jobs;
    }
    
    public static int createJob(String title, String description) throws SQLException {
        // Check if job title already exists
        String checkSql = "SELECT id FROM job_titles WHERE title = ?";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, title);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    throw new SQLException("Jobul cu titlul '" + title + "' există deja");
                }
            }
        }
        
        String sql = "INSERT INTO job_titles (title, description) VALUES (?, ?) RETURNING id";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            if (description != null && !description.trim().isEmpty()) {
                ps.setString(2, description);
            } else {
                ps.setNull(2, java.sql.Types.VARCHAR);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        throw new SQLException("Nu s-a putut crea jobul");
    }
    
    public static boolean updateJob(int jobId, String title, String description) throws SQLException {
        // Check if another job with this title exists
        String checkSql = "SELECT id FROM job_titles WHERE title = ? AND id != ?";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, title);
            ps.setInt(2, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    throw new SQLException("Un alt job cu titlul '" + title + "' există deja");
                }
            }
        }
        
        String sql = "UPDATE job_titles SET title = ?, description = ? WHERE id = ?";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            if (description != null && !description.trim().isEmpty()) {
                ps.setString(2, description);
            } else {
                ps.setNull(2, java.sql.Types.VARCHAR);
            }
            ps.setInt(3, jobId);
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    public static boolean deleteJob(int jobId) throws SQLException {
        String sql = "DELETE FROM job_titles WHERE id = ?";
        
        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }
}
