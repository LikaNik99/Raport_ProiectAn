package md.uzina.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthService {
    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

    // Returneaza User sau null
    public static User authenticate(String idStr, String password) {
        try {
            int id = Integer.parseInt(idStr);
            String sql = "SELECT id, name, role, password_hash FROM users WHERE id = ?";
            try (Connection c = DBManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String hash = rs.getString("password_hash");
                        // in acest exemplu simplu, parola stocata in cleartext (NU recomandat).
                        // Daca folosești hash, verifică aici.
                        if (hash != null && hash.equals(password)) {
                            User u = new User(rs.getInt("id"), rs.getString("name"), rs.getString("role"));
                            // Log the login event
                            try {
                                LoginHistoryService.logLogin(u.id, u.name, u.role);
                            } catch (Exception ex) {
                                LOG.warn("Failed to log login event", ex);
                            }
                            return u;
                        }
                    }
                }
            }
        } catch (NumberFormatException nfe) {
            // invalid id format -> treat as authentication failure
            LOG.debug("authenticate: invalid id format '{}'", idStr);
        } catch (Exception e) {
            // log exception without printing stacktrace to stdout
            LOG.error("authenticate: unexpected error", e);
        }
        return null;
    }

    public static boolean markPresence(int userId) {
        String sql = "INSERT INTO presence (user_id, time_in) VALUES (?, NOW())";
        try (Connection c = DBManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            LOG.error("markPresence: database error", ex);
            return false;
        }
    }
}
