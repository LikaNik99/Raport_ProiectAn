package com.blackjack.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class GameSessionDAO {
    private static final Logger logger = LoggerFactory.getLogger(GameSessionDAO.class);
    private DatabaseManager dbManager;

    public GameSessionDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Creaza o sesiune de joc noua
     * Returneaza ID-ul sesiunii
     */
    public int createSession(String roomId) {
        String sql = "INSERT INTO game_sessions (room_id, start_time) VALUES (?, NOW())";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, roomId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int sessionId = rs.getInt(1);
                    logger.info("Created game session: {} for room: {}", sessionId, roomId);
                    return sessionId;
                }
            }

        } catch (SQLException e) {
            logger.error("Error creating game session for room: {}", roomId, e);
        }

        return -1;
    }

    /**
     * Incheie o sesiune de joc cu informatii despre castigator si pot
     */
    public boolean endSession(int sessionId, Integer winnerId, double totalPot) {
        String sql = "UPDATE game_sessions SET end_time = NOW(), winner_id = ?, total_pot = ? " +
                     "WHERE session_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (winnerId != null) {
                stmt.setInt(1, winnerId);
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setDouble(2, totalPot);
            stmt.setInt(3, sessionId);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Ended game session: {}", sessionId);
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error ending game session: {}", sessionId, e);
            return false;
        }
    }

    /**
     * Adauga un jucator la o sesiune cu pariul si rezultatul sau
     */
    public boolean addPlayerToSession(int sessionId, int userId, double betAmount,
                                      String result, double winnings) {
        String sql = "INSERT INTO session_players (session_id, user_id, bet_amount, result, winnings) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, sessionId);
            stmt.setInt(2, userId);
            stmt.setDouble(3, betAmount);
            stmt.setString(4, result);
            stmt.setDouble(5, winnings);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error adding player to session: {}, user: {}", sessionId, userId, e);
            return false;
        }
    }

    /**
     * Obtine numarul total de sesiuni jucate
     */
    public int getTotalSessions() {
        String sql = "SELECT COUNT(*) as count FROM game_sessions WHERE end_time IS NOT NULL";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            logger.error("Error getting total sessions", e);
        }

        return 0;
    }

    /**
     * Obtine potul total distribuit
     */
    public double getTotalPotDistributed() {
        String sql = "SELECT SUM(total_pot) as total FROM game_sessions WHERE end_time IS NOT NULL";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getDouble("total");
            }

        } catch (SQLException e) {
            logger.error("Error getting total pot distributed", e);
        }

        return 0.0;
    }
}
