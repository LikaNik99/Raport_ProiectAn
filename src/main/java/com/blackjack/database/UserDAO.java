package com.blackjack.database;

import com.blackjack.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);
    private DatabaseManager dbManager;

    public UserDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Creaza un utilizator nou in baza de date
     */
    public boolean createUser(String username, String passwordHash) {
        String sql = "INSERT INTO users (username, password_hash, balance, points) VALUES (?, ?, 1000.00, 0)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, passwordHash);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Created user: {}", username);
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error creating user: {}", username, e);
            return false;
        }
    }

    /**
     * Obtine utilizatorul dupa nume utilizator
     */
    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }

        } catch (SQLException e) {
            logger.error("Error getting user by username: {}", username, e);
        }

        return null;
    }

    /**
     * Obtine utilizatorul dupa ID utilizator
     */
    public User getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }

        } catch (SQLException e) {
            logger.error("Error getting user by ID: {}", userId, e);
        }

        return null;
    }

    /**
     * Actualizeaza balanta utilizatorului
     */
    public boolean updateBalance(int userId, double balance) {
        String sql = "UPDATE users SET balance = ? WHERE user_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, balance);
            stmt.setInt(2, userId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error updating balance for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Actualizeaza punctele utilizatorului
     */
    public boolean updatePoints(int userId, int points) {
        String sql = "UPDATE users SET points = ? WHERE user_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, points);
            stmt.setInt(2, userId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error updating points for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Inregistreaza rezultatul jocului (castig sau pierdere)
     */
    public boolean recordGameResult(int userId, boolean won, double balanceChange) {
        String sql;
        if (won) {
            sql = "UPDATE users SET wins = wins + 1, total_games = total_games + 1, " +
                  "balance = balance + ?, points = points + ? WHERE user_id = ?";
        } else {
            sql = "UPDATE users SET losses = losses + 1, total_games = total_games + 1, " +
                  "balance = balance + ? WHERE user_id = ?";
        }

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, balanceChange);

            if (won) {
                int pointsEarned = (int) (Math.abs(balanceChange) / 10);
                stmt.setInt(2, pointsEarned);
                stmt.setInt(3, userId);
            } else {
                stmt.setInt(2, userId);
            }

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error recording game result for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Actualizeaza statisticile utilizatorului dupa un joc
     */
    public boolean updateUserStats(User user) {
        String sql = "UPDATE users SET balance = ?, points = ?, wins = ?, " +
                     "losses = ?, total_games = ? WHERE user_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, user.getBalance());
            stmt.setInt(2, user.getPoints());
            stmt.setInt(3, user.getWins());
            stmt.setInt(4, user.getLosses());
            stmt.setInt(5, user.getTotalGames());
            stmt.setInt(6, user.getUserId());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error updating user stats: {}", user.getUserId(), e);
            return false;
        }
    }

    /**
     * Obtine cei mai buni jucatori dupa puncte (pentru clasament)
     */
    public List<User> getLeaderboard(int limit) {
        String sql = "SELECT * FROM users ORDER BY points DESC LIMIT ?";
        List<User> leaderboard = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                leaderboard.add(extractUserFromResultSet(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting leaderboard", e);
        }

        return leaderboard;
    }

    /**
     * Obtine rangul utilizatorului in clasament
     */
    public int getUserRank(int userId) {
        String sql = "SELECT COUNT(*) + 1 as rank FROM users " +
                     "WHERE points > (SELECT points FROM users WHERE user_id = ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("rank");
            }

        } catch (SQLException e) {
            logger.error("Error getting user rank: {}", userId, e);
        }

        return -1;
    }

    /**
     * Verifica daca numele de utilizator exista
     */
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) as count FROM users WHERE username = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }

        } catch (SQLException e) {
            logger.error("Error checking if username exists: {}", username, e);
        }

        return false;
    }

    /**
     * Extrage obiectul User din ResultSet
     */
    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setBalance(rs.getDouble("balance"));
        user.setPoints(rs.getInt("points"));
        user.setTotalGames(rs.getInt("total_games"));
        user.setWins(rs.getInt("wins"));
        user.setLosses(rs.getInt("losses"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }
}
