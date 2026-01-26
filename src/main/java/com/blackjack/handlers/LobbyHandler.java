package com.blackjack.handlers;

import com.blackjack.database.GameSessionDAO;
import com.blackjack.database.UserDAO;
import com.blackjack.game.GameRoom;
import com.blackjack.models.User;
import com.blackjack.utils.JsonUtils;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class LobbyHandler {
    private static final Logger logger = LoggerFactory.getLogger(LobbyHandler.class);
    private Map<String, GameRoom> gameRooms;
    private Queue<User> quickMatchQueue;
    private UserDAO userDAO;
    private GameSessionDAO gameSessionDAO;

    public LobbyHandler(Map<String, GameRoom> gameRooms, Queue<User> quickMatchQueue) {
        this.gameRooms = gameRooms;
        this.quickMatchQueue = quickMatchQueue;
        this.userDAO = new UserDAO();
        this.gameSessionDAO = new GameSessionDAO();
    }

    /**
     * Gestioneaza cererea de potrivire rapida
     */
    public void handleQuickMatch(WebSocket conn, JsonObject data) {
        try {
            int userId = data.get("userId").getAsInt();
            User user = userDAO.getUserById(userId);

            if (user == null) {
                sendError(conn, "User not found", "USER_NOT_FOUND");
                return;
            }

            // Verifica daca utilizatorul este deja intr-o camera
            for (GameRoom room : gameRooms.values()) {
                if (room.findPlayerByUserId(userId) != null) {
                    sendError(conn, "Already in a game", "ALREADY_IN_GAME");
                    return;
                }
            }

            // Incearca sa gaseasca o camera disponibila
            GameRoom availableRoom = null;
            for (GameRoom room : gameRooms.values()) {
                if (!room.isFull() && room.getCurrentPhase() == GameRoom.GamePhase.WAITING) {
                    availableRoom = room;
                    break;
                }
            }

            // Daca nu este disponibila nicio camera, creaza una noua
            if (availableRoom == null) {
                availableRoom = new GameRoom(5, 10.0); // 5 jucatori maxim, 10 pariu minim
                gameRooms.put(availableRoom.getRoomId(), availableRoom);

                // Creaza sesiunea in baza de date
                int sessionId = gameSessionDAO.createSession(availableRoom.getRoomId());
                availableRoom.setSessionId(sessionId);

                logger.info("Created new room: {}", availableRoom.getRoomId());
            }

            // Adauga jucatorul in camera
            boolean added = availableRoom.addPlayer(user, conn);
            if (!added) {
                sendError(conn, "Failed to join room", "JOIN_FAILED");
                return;
            }

            // Trimite atribuirea camerei catre jucator
            sendRoomAssignment(conn, availableRoom);

            // Daca camera are cel putin 1 jucator, porneste jocul
            if (availableRoom.getPlayerCount() >= 1) {
                availableRoom.startGame();
                broadcastGameState(availableRoom);
            }

            logger.info("Player {} joined room {} via quick match", userId, availableRoom.getRoomId());

        } catch (Exception e) {
            logger.error("Error handling quick match", e);
            sendError(conn, "Error during quick match", "QUICK_MATCH_ERROR");
        }
    }

    /**
     * Gestioneaza cererea de a se alatura unei camere specifice
     */
    public void handleJoinRoom(WebSocket conn, JsonObject data) {
        try {
            int userId = data.get("userId").getAsInt();
            String roomId = data.get("roomId").getAsString();

            User user = userDAO.getUserById(userId);
            if (user == null) {
                sendError(conn, "User not found", "USER_NOT_FOUND");
                return;
            }

            GameRoom room = gameRooms.get(roomId);
            if (room == null) {
                sendError(conn, "Room not found", "ROOM_NOT_FOUND");
                return;
            }

            if (room.isFull()) {
                sendError(conn, "Room is full", "ROOM_FULL");
                return;
            }

            if (room.getCurrentPhase() != GameRoom.GamePhase.WAITING) {
                sendError(conn, "Game already in progress", "GAME_IN_PROGRESS");
                return;
            }

            boolean added = room.addPlayer(user, conn);
            if (!added) {
                sendError(conn, "Failed to join room", "JOIN_FAILED");
                return;
            }

            sendRoomAssignment(conn, room);

            // Daca camera are suficienti jucatori, porneste jocul
            if (room.getPlayerCount() >= 1) {
                room.startGame();
                broadcastGameState(room);
            }

            logger.info("Player {} joined room {}", userId, roomId);

        } catch (Exception e) {
            logger.error("Error handling join room", e);
            sendError(conn, "Error joining room", "JOIN_ROOM_ERROR");
        }
    }

    /**
     * Gestioneaza cererea de obtinere a listei de servere
     */
    public void handleGetServers(WebSocket conn) {
        try {
            StringBuilder roomsJson = new StringBuilder();
            roomsJson.append("[");

            boolean first = true;
            for (GameRoom room : gameRooms.values()) {
                if (room.getCurrentPhase() == GameRoom.GamePhase.WAITING && !room.isEmpty()) {
                    if (!first) roomsJson.append(",");

                    roomsJson.append(String.format(
                        "{\"roomId\":\"%s\",\"playerCount\":%d,\"maxPlayers\":%d,\"minBet\":%.2f,\"status\":\"waiting\"}",
                        room.getRoomId(), room.getPlayerCount(), room.getMaxPlayers(), room.getMinBet()
                    ));

                    first = false;
                }
            }

            roomsJson.append("]");

            String response = JsonUtils.createMessage("SERVER_LIST",
                "{\"servers\":" + roomsJson.toString() + "}");
            conn.send(response);

            logger.info("Sent server list to client");

        } catch (Exception e) {
            logger.error("Error handling get servers", e);
            sendError(conn, "Error getting server list", "GET_SERVERS_ERROR");
        }
    }

    /**
     * Gestioneaza cererea de obtinere a clasamentului
     */
    public void handleGetLeaderboard(WebSocket conn, JsonObject data) {
        try {
            int limit = 10;
            if (data.has("limit")) {
                limit = data.get("limit").getAsInt();
            }

            List<User> topPlayers = userDAO.getLeaderboard(limit);

            StringBuilder leaderboardJson = new StringBuilder();
            leaderboardJson.append("[");

            for (int i = 0; i < topPlayers.size(); i++) {
                if (i > 0) leaderboardJson.append(",");

                User user = topPlayers.get(i);
                leaderboardJson.append(String.format(
                    "{\"rank\":%d,\"username\":\"%s\",\"points\":%d,\"wins\":%d,\"totalGames\":%d,\"winRate\":%.2f}",
                    i + 1, user.getUsername(), user.getPoints(), user.getWins(),
                    user.getTotalGames(), user.getWinRate()
                ));
            }

            leaderboardJson.append("]");

            String response = JsonUtils.createMessage("LEADERBOARD",
                "{\"leaders\":" + leaderboardJson.toString() + "}");
            conn.send(response);

            logger.info("Sent leaderboard to client");

        } catch (Exception e) {
            logger.error("Error handling get leaderboard", e);
            sendError(conn, "Error getting leaderboard", "LEADERBOARD_ERROR");
        }
    }

    /**
     * Gestioneaza jucatorul care paraseste camera
     */
    public void handleLeaveRoom(int userId) {
        for (GameRoom room : new ArrayList<>(gameRooms.values())) {
            if (room.removePlayer(userId)) {
                logger.info("Player {} left room {}", userId, room.getRoomId());

                // Daca camera este goala, sterge-o
                if (room.isEmpty()) {
                    gameRooms.remove(room.getRoomId());
                    logger.info("Removed empty room: {}", room.getRoomId());
                } else {
                    // Transmite starea actualizata catre jucatorii ramasi
                    broadcastGameState(room);
                }
                break;
            }
        }
    }

    /**
     * Trimite atribuirea camerei catre jucator
     */
    private void sendRoomAssignment(WebSocket conn, GameRoom room) {
        String roomData = String.format(
            "{\"roomId\":\"%s\",\"maxPlayers\":%d,\"minBet\":%.2f}",
            room.getRoomId(), room.getMaxPlayers(), room.getMinBet()
        );

        String response = JsonUtils.successResponse("ROOM_ASSIGNED",
            "Joined room successfully", roomData);
        conn.send(response);
    }

    /**
     * Transmite starea jocului catre toti jucatorii din camera
     */
    private void broadcastGameState(GameRoom room) {
        String gameState = JsonUtils.createMessage("GAME_STATE", room.toJson());
        room.getPlayers().forEach(player -> {
            if (player.isConnected()) {
                player.getConnection().send(gameState);
            }
        });
    }

    /**
     * Trimite mesaj de eroare
     */
    private void sendError(WebSocket conn, String message, String code) {
        String error = JsonUtils.errorResponse(message, code);
        conn.send(error);
    }
}
