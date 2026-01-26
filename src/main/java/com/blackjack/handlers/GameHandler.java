package com.blackjack.handlers;

import com.blackjack.database.GameSessionDAO;
import com.blackjack.database.UserDAO;
import com.blackjack.game.GameRoom;
import com.blackjack.game.Player;
import com.blackjack.utils.JsonUtils;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class GameHandler {
    private static final Logger logger = LoggerFactory.getLogger(GameHandler.class);
    private Map<String, GameRoom> gameRooms;
    private UserDAO userDAO;
    private GameSessionDAO gameSessionDAO;

    public GameHandler(Map<String, GameRoom> gameRooms) {
        this.gameRooms = gameRooms;
        this.userDAO = new UserDAO();
        this.gameSessionDAO = new GameSessionDAO();
    }

    /**
     * Gestioneaza cererea de plasare pariu
     */
    public void handlePlaceBet(WebSocket conn, JsonObject data) {
        try {
            int userId = data.get("userId").getAsInt();
            String roomId = data.get("roomId").getAsString();
            double amount = data.get("amount").getAsDouble();

            GameRoom room = gameRooms.get(roomId);
            if (room == null) {
                sendError(conn, "Room not found", "ROOM_NOT_FOUND");
                return;
            }

            Player player = room.findPlayerByUserId(userId);
            if (player == null) {
                sendError(conn, "Player not in room", "PLAYER_NOT_FOUND");
                return;
            }

            if (!player.isConnected() || player.getConnection() != conn) {
                sendError(conn, "Invalid connection", "INVALID_CONNECTION");
                return;
            }

            boolean success = room.placeBet(userId, amount);
            if (!success) {
                sendError(conn, "Failed to place bet. Check balance and bet amount", "BET_FAILED");
                return;
            }

            // Salveaza balanta actualizata in baza de date imediat
            userDAO.updateBalance(player.getUserId(), player.getBalance());
            logger.info("Player {} balance saved to database: ${}", userId, player.getBalance());

            // Transmite starea actualizata a jocului catre toti jucatorii din camera
            broadcastGameState(room);

            logger.info("Player {} placed bet of {} in room {}", userId, amount, roomId);

        } catch (Exception e) {
            logger.error("Error handling place bet", e);
            sendError(conn, "Error placing bet", "BET_ERROR");
        }
    }

    /**
     * Gestioneaza cererea de tragere carte
     */
    public void handleHit(WebSocket conn, JsonObject data) {
        try {
            int userId = data.get("userId").getAsInt();
            String roomId = data.get("roomId").getAsString();

            GameRoom room = gameRooms.get(roomId);
            if (room == null) {
                sendError(conn, "Room not found", "ROOM_NOT_FOUND");
                return;
            }

            boolean success = room.playerHit(userId);
            if (!success) {
                sendError(conn, "Cannot hit at this time", "HIT_FAILED");
                return;
            }

            // Transmite starea actualizata a jocului
            broadcastGameState(room);

            // Verifica daca jocul este terminat
            if (room.getCurrentPhase() == GameRoom.GamePhase.FINISHED) {
                handleGameEnd(room);
            }

            logger.info("Player {} hit in room {}", userId, roomId);

        } catch (Exception e) {
            logger.error("Error handling hit", e);
            sendError(conn, "Error processing hit", "HIT_ERROR");
        }
    }

    /**
     * Gestioneaza cererea de oprire
     */
    public void handleStand(WebSocket conn, JsonObject data) {
        try {
            int userId = data.get("userId").getAsInt();
            String roomId = data.get("roomId").getAsString();

            GameRoom room = gameRooms.get(roomId);
            if (room == null) {
                sendError(conn, "Room not found", "ROOM_NOT_FOUND");
                return;
            }

            boolean success = room.playerStand(userId);
            if (!success) {
                sendError(conn, "Cannot stand at this time", "STAND_FAILED");
                return;
            }

            // Transmite starea actualizata a jocului
            broadcastGameState(room);

            // Verifica daca jocul este terminat
            if (room.getCurrentPhase() == GameRoom.GamePhase.FINISHED) {
                handleGameEnd(room);
            }

            logger.info("Player {} stood in room {}", userId, roomId);

        } catch (Exception e) {
            logger.error("Error handling stand", e);
            sendError(conn, "Error processing stand", "STAND_ERROR");
        }
    }

    /**
     * Gestioneaza sfarsitul jocului - salveaza in baza de date si trimite rezultate
     */
    private void handleGameEnd(GameRoom room) {
        try {
            // Incheie sesiunea in baza de date
            if (room.getSessionId() > 0) {
                gameSessionDAO.endSession(room.getSessionId(), null, room.getPot());

                // Salveaza rezultatele jucatorilor
                for (Player player : room.getPlayers()) {
                    String result = "lose";
                    double winnings = 0;

                    if (player.getStatus() == Player.PlayerStatus.WON) {
                        result = "win";
                        winnings = player.getUser().getBalance() - player.getCurrentBet();
                    } else if (player.getStatus() == Player.PlayerStatus.PUSH) {
                        result = "push";
                    }

                    gameSessionDAO.addPlayerToSession(
                        room.getSessionId(),
                        player.getUserId(),
                        player.getCurrentBet(),
                        result,
                        winnings
                    );

                    // Actualizeaza utilizatorul in baza de date
                    userDAO.updateUserStats(player.getUser());
                }
            }

            // Trimite rezultatul jocului catre toti jucatorii
            broadcastGameResults(room);

            logger.info("Game ended in room: {}", room.getRoomId());

        } catch (Exception e) {
            logger.error("Error handling game end", e);
        }
    }

    /**
     * Transmite starea curenta a jocului catre toti jucatorii din camera
     */
    private void broadcastGameState(GameRoom room) {
        String gameState = JsonUtils.createMessage("GAME_STATE", room.toJson());
        for (Player player : room.getPlayers()) {
            if (player.isConnected()) {
                player.getConnection().send(gameState);
            }
        }
    }

    /**
     * Transmite rezultatele jocului catre toti jucatorii
     */
    private void broadcastGameResults(GameRoom room) {
        StringBuilder resultsJson = new StringBuilder();
        resultsJson.append("{\"dealerHand\":").append(room.getDealer().getHand().toJson());
        resultsJson.append(",\"dealerValue\":").append(room.getDealer().getHandValue());
        resultsJson.append(",\"playerResults\":[");

        boolean first = true;
        for (Player player : room.getPlayers()) {
            if (!first) resultsJson.append(",");
            resultsJson.append(String.format(
                "{\"userId\":%d,\"username\":\"%s\",\"handValue\":%d,\"result\":\"%s\",\"balance\":%.2f}",
                player.getUserId(), player.getUsername(), player.getHandValue(),
                player.getStatus().toString().toLowerCase(), player.getBalance()
            ));
            first = false;
        }

        resultsJson.append("]}");

        String resultMessage = JsonUtils.createMessage("GAME_RESULT", resultsJson.toString());
        for (Player player : room.getPlayers()) {
            if (player.isConnected()) {
                player.getConnection().send(resultMessage);
            }
        }
    }

    /**
     * Trimite mesaj de eroare
     */
    private void sendError(WebSocket conn, String message, String code) {
        String error = JsonUtils.errorResponse(message, code);
        conn.send(error);
    }
}
