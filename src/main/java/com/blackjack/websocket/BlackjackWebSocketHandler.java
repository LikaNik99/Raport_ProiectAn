package com.blackjack.websocket;

import com.blackjack.game.GameRoom;
import com.blackjack.models.User;
import com.blackjack.service.AuthService;
import com.blackjack.service.GameSessionService;
import com.blackjack.service.UserService;
import com.blackjack.utils.JsonUtils;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;

@Component
public class BlackjackWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(BlackjackWebSocketHandler.class);

    private final Map<String, GameRoom> gameRooms = new ConcurrentHashMap<>();
    private final Map<String, User> connectedClients = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<Integer, String> userRooms = new ConcurrentHashMap<>(); // userId -> roomId
    private final Queue<User> quickMatchQueue = new LinkedList<>();

    private final AuthService authService;
    private final UserService userService;
    private final GameSessionService gameSessionService;

    @Autowired
    public BlackjackWebSocketHandler(AuthService authService,
                                     UserService userService,
                                     GameSessionService gameSessionService) {
        this.authService = authService;
        this.userService = userService;
        this.gameSessionService = gameSessionService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("New connection established: {}", session.getId());
        sessions.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            logger.debug("Received message from {}: {}", session.getId(), payload);

            JsonObject json = JsonUtils.parse(payload);
            String type = json.get("type").getAsString();
            JsonObject data = json.has("data") ? json.getAsJsonObject("data") : new JsonObject();

            // Rutare mesaj catre gestionarul corespunzator
            switch (type) {
                case "LOGIN":
                    handleLogin(session, data);
                    break;

                case "REGISTER":
                    handleRegister(session, data);
                    break;

                case "QUICK_MATCH":
                    handleQuickMatch(session, data);
                    break;

                case "JOIN_ROOM":
                    handleJoinRoom(session, data);
                    break;

                case "CREATE_ROOM":
                    handleCreateRoom(session, data);
                    break;

                case "GET_SERVERS":
                    handleGetServers(session);
                    break;

                case "GET_LEADERBOARD":
                    handleGetLeaderboard(session, data);
                    break;

                case "PLACE_BET":
                    handlePlaceBet(session, data);
                    break;

                case "HIT":
                    handleHit(session, data);
                    break;

                case "STAND":
                    handleStand(session, data);
                    break;

                case "SPLIT":
                    handleSplit(session, data);
                    break;

                case "DOUBLE_DOWN":
                    handleDoubleDown(session, data);
                    break;

                case "INSURANCE":
                    handleInsurance(session, data);
                    break;

                case "PLAY_AGAIN":
                    handlePlayAgain(session, data);
                    break;

                case "LEAVE_ROOM":
                    int userId = data.get("userId").getAsInt();
                    handleLeaveRoom(session, userId);
                    break;

                case "CHAT_MESSAGE":
                    handleChatMessage(session, data);
                    break;

                default:
                    logger.warn("Unknown message type: {}", type);
                    sendError(session, "Unknown message type", "UNKNOWN_TYPE");
            }

        } catch (Exception e) {
            logger.error("Error processing message", e);
            sendError(session, "Error processing message: " + e.getMessage(), "MESSAGE_ERROR");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("Connection closed: {} - Reason: {}", session.getId(), status.getReason());

        User user = connectedClients.get(session.getId());
        if (user != null) {
            handleLeaveRoom(session, user.getUserId());
            connectedClients.remove(session.getId());
            sessions.remove(session.getId());
            logger.info("User {} disconnected", user.getUsername());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error for session: {}", session.getId(), exception);
    }

    // Handler methods
    private void handleLogin(WebSocketSession session, JsonObject data) {
        try {
            String username = data.get("username").getAsString();
            String password = data.get("password").getAsString();

            User user = authService.handleLogin(username, password);
            connectedClients.put(session.getId(), user);

            sendLoginSuccess(session, user);

        } catch (IllegalArgumentException e) {
            sendLoginError(session, e.getMessage());
        } catch (Exception e) {
            logger.error("Error handling login", e);
            sendLoginError(session, "An error occurred during login");
        }
    }

    private void handleRegister(WebSocketSession session, JsonObject data) {
        try {
            String username = data.get("username").getAsString();
            String password = data.get("password").getAsString();

            User user = authService.handleRegister(username, password);
            connectedClients.put(session.getId(), user);

            sendRegisterSuccess(session, user);

        } catch (IllegalArgumentException e) {
            sendRegisterError(session, e.getMessage());
        } catch (Exception e) {
            logger.error("Error handling registration", e);
            sendRegisterError(session, "An error occurred during registration");
        }
    }

    private void handleQuickMatch(WebSocketSession session, JsonObject data) {
        try {
            User user = connectedClients.get(session.getId());
            if (user == null) {
                sendError(session, "User not authenticated", "NOT_AUTHENTICATED");
                return;
            }

            // Gaseste o camera disponibila sau creaza una noua
            GameRoom room = findOrCreateRoom();

            // Reseteaza camera daca este in faza FINISHED (jocul anterior s-a terminat)
            if (room.getCurrentPhase() == GameRoom.GamePhase.FINISHED ||
                room.getCurrentPhase() == GameRoom.GamePhase.RESULTS) {
                room.resetGame();
                logger.info("Room {} reset to WAITING (was in finished state)", room.getRoomId());
            }

            // Adauga jucatorul in camera (transmite null pentru conexiunea WebSocket deoarece gestionam sesiunile separat)
            if (room.addPlayer(user, null)) {
                userRooms.put(user.getUserId(), room.getRoomId());
                logger.info("User {} joined room {}", user.getUsername(), room.getRoomId());

                // Trimite raspunsul de atribuire camera
                sendRoomAssignment(session, room);

                // Daca camera are suficienti jucatori, porneste jocul
                if (room.getPlayerCount() >= 1) {
                    room.startGame();
                    broadcastGameState(room);
                }
            } else {
                sendError(session, "Failed to join room", "ROOM_JOIN_FAILED");
            }

        } catch (Exception e) {
            logger.error("Error handling quick match", e);
            sendError(session, "An error occurred during quick match", "QUICKMATCH_ERROR");
        }
    }

    private GameRoom findOrCreateRoom() {
        // Cauta o camera disponibila (faza WAITING sau BETTING - inainte ca cartile sa fie distribuite)
        for (GameRoom room : gameRooms.values()) {
            if (!room.isFull() &&
                (room.getCurrentPhase() == GameRoom.GamePhase.WAITING ||
                 room.getCurrentPhase() == GameRoom.GamePhase.BETTING)) {
                return room;
            }
        }

        // Nicio camera disponibila gasita, creaza una noua
        GameRoom newRoom = new GameRoom(5, 10.0); // Maxim 5 jucatori, pariu minim $10
        gameRooms.put(newRoom.getRoomId(), newRoom);
        logger.info("Created new game room: {}", newRoom.getRoomId());
        return newRoom;
    }

    private void sendRoomAssignment(WebSocketSession session, GameRoom room) {
        try {
            String roomData = String.format(Locale.US,
                "{\"roomId\":\"%s\",\"playerCount\":%d,\"maxPlayers\":%d,\"minBet\":%.2f}",
                room.getRoomId(), room.getPlayerCount(), room.getMaxPlayers(), room.getMinBet()
            );

            String response = JsonUtils.successResponse("ROOM_ASSIGNED", "Joined room successfully", roomData);
            session.sendMessage(new TextMessage(response));
        } catch (Exception e) {
            logger.error("Error sending room assignment", e);
        }
    }

    private void broadcastGameState(GameRoom room) {
        try {
            String gameStateJson = room.toJson();
            String message = JsonUtils.createMessage("GAME_STATE", gameStateJson);

            // Trimite catre toti jucatorii din camera
            for (com.blackjack.game.Player player : room.getPlayers()) {
                WebSocketSession playerSession = findSessionByUserId(player.getUserId());
                if (playerSession != null && playerSession.isOpen()) {
                    playerSession.sendMessage(new TextMessage(message));
                }
            }

            // Daca jocul este terminat, trimite rezultatele jocului
            if (room.getCurrentPhase() == GameRoom.GamePhase.FINISHED ||
                room.getCurrentPhase() == GameRoom.GamePhase.RESULTS) {
                broadcastGameResult(room);
            }
        } catch (Exception e) {
            logger.error("Error broadcasting game state", e);
        }
    }

    private void broadcastGameResult(GameRoom room) {
        try {
            String resultJson = room.getGameResultJson();
            String message = JsonUtils.createMessage("GAME_RESULT", resultJson);

            // Salveaza statisticile utilizatorului in baza de date pentru toti jucatorii
            for (com.blackjack.game.Player player : room.getPlayers()) {
                User user = player.getUser();

                // Salveaza statisticile actualizate ale utilizatorului in baza de date
                userService.updateUserStats(user);
                logger.info("Saved stats for user {}: balance=${}, points={}, wins={}, losses={}, totalGames={}",
                    user.getUsername(), user.getBalance(), user.getPoints(),
                    user.getWins(), user.getLosses(), user.getTotalGames());

                // Trimite rezultatul catre jucator
                WebSocketSession playerSession = findSessionByUserId(player.getUserId());
                if (playerSession != null && playerSession.isOpen()) {
                    playerSession.sendMessage(new TextMessage(message));
                }
            }

            logger.info("Game result broadcast for room {}", room.getRoomId());
        } catch (Exception e) {
            logger.error("Error broadcasting game result", e);
        }
    }

    private WebSocketSession findSessionByUserId(int userId) {
        for (Map.Entry<String, User> entry : connectedClients.entrySet()) {
            if (entry.getValue().getUserId() == userId) {
                return sessions.get(entry.getKey());
            }
        }
        return null;
    }

    private void handleJoinRoom(WebSocketSession session, JsonObject data) {
        try {
            User user = connectedClients.get(session.getId());
            if (user == null) {
                sendError(session, "User not authenticated", "NOT_AUTHENTICATED");
                return;
            }

            String roomId = data.get("roomId").getAsString();
            String password = data.has("password") ? data.get("password").getAsString() : "";

            GameRoom room = gameRooms.get(roomId);

            if (room == null) {
                sendError(session, "Room not found", "ROOM_NOT_FOUND");
                return;
            }

            if (room.isFull()) {
                sendError(session, "Room is full", "ROOM_FULL");
                return;
            }

            // Verifica parola daca camera este protejata cu parola
            if (!room.checkPassword(password)) {
                sendError(session, "Incorrect password", "INCORRECT_PASSWORD");
                return;
            }

            // Reseteaza camera daca este in faza FINISHED (jocul anterior s-a terminat)
            if (room.getCurrentPhase() == GameRoom.GamePhase.FINISHED ||
                room.getCurrentPhase() == GameRoom.GamePhase.RESULTS) {
                room.resetGame();
                logger.info("Room {} reset to WAITING (was in finished state)", room.getRoomId());
            }

            if (room.addPlayer(user, null)) {
                userRooms.put(user.getUserId(), room.getRoomId());
                logger.info("User {} joined room {}", user.getUsername(), room.getRoomId());

                sendRoomAssignment(session, room);

                // Daca camera are suficienti jucatori, porneste jocul
                if (room.getPlayerCount() >= 1) {
                    room.startGame();
                    broadcastGameState(room);
                }
            } else {
                sendError(session, "Failed to join room", "ROOM_JOIN_FAILED");
            }
        } catch (Exception e) {
            logger.error("Error handling join room", e);
            sendError(session, "An error occurred while joining room", "JOIN_ROOM_ERROR");
        }
    }

    private void handleCreateRoom(WebSocketSession session, JsonObject data) {
        try {
            User user = connectedClients.get(session.getId());
            if (user == null) {
                sendError(session, "User not authenticated", "NOT_AUTHENTICATED");
                return;
            }

            // Obtine parametrii camerei
            String serverName = data.has("serverName") ? data.get("serverName").getAsString() : "";
            String password = data.has("password") ? data.get("password").getAsString() : "";

            // Valideaza numele serverului
            if (serverName == null || serverName.trim().isEmpty()) {
                sendError(session, "Server name is required", "INVALID_SERVER_NAME");
                return;
            }

            if (serverName.length() < 3 || serverName.length() > 30) {
                sendError(session, "Server name must be between 3 and 30 characters", "INVALID_SERVER_NAME");
                return;
            }

            // Valideaza parola daca este furnizata
            if (password != null && !password.isEmpty() && password.length() < 3) {
                sendError(session, "Password must be at least 3 characters", "INVALID_PASSWORD");
                return;
            }

            // Creaza o camera noua cu setari personalizate
            int maxPlayers = 5; // Maxim jucatori implicit
            double minBet = 10.0; // Pariu minim implicit

            GameRoom room = new GameRoom(maxPlayers, minBet, serverName, password);
            gameRooms.put(room.getRoomId(), room);

            logger.info("User {} created room {} ({})", user.getUsername(), room.getRoomId(), serverName);

            // Adauga creatorul in camera
            if (room.addPlayer(user, null)) {
                userRooms.put(user.getUserId(), room.getRoomId());

                // Trimite raspuns creare camera
                String responseData = String.format(Locale.US,
                    "{\"roomId\":\"%s\",\"serverName\":\"%s\",\"hasPassword\":%b}",
                    room.getRoomId(), room.getServerName(), room.hasPassword()
                );
                String response = JsonUtils.successResponse("CREATE_ROOM_RESPONSE", "Room created successfully", responseData);
                session.sendMessage(new TextMessage(response));

                // Trimite atribuirea camerei
                sendRoomAssignment(session, room);

                // Porneste jocul daca sunt suficienti jucatori (in acest caz, doar 1)
                if (room.getPlayerCount() >= 1) {
                    room.startGame();
                    broadcastGameState(room);
                }
            } else {
                sendError(session, "Failed to join created room", "ROOM_JOIN_FAILED");
            }

        } catch (Exception e) {
            logger.error("Error handling create room", e);
            sendError(session, "An error occurred while creating room", "CREATE_ROOM_ERROR");
        }
    }

    private void handleGetServers(WebSocketSession session) {
        try {
            StringBuilder serversJson = new StringBuilder("[");
            int index = 0;

            for (GameRoom room : gameRooms.values()) {
                // Only list rooms that are in WAITING or BETTING phase and not full
                if (!room.isFull() &&
                    (room.getCurrentPhase() == GameRoom.GamePhase.WAITING ||
                     room.getCurrentPhase() == GameRoom.GamePhase.BETTING)) {

                    if (index > 0) serversJson.append(",");

                    String serverName = room.getServerName() != null && !room.getServerName().isEmpty()
                        ? room.getServerName() : room.getRoomId();

                    serversJson.append(String.format(Locale.US,
                        "{\"roomId\":\"%s\",\"serverName\":\"%s\",\"playerCount\":%d,\"maxPlayers\":%d,\"minBet\":%.2f,\"status\":\"waiting\",\"hasPassword\":%b}",
                        room.getRoomId(), serverName, room.getPlayerCount(), room.getMaxPlayers(), room.getMinBet(), room.hasPassword()
                    ));
                    index++;
                }
            }
            serversJson.append("]");

            String response = JsonUtils.successResponse("SERVER_LIST", "Server list retrieved",
                "{\"servers\":" + serversJson.toString() + "}");
            session.sendMessage(new TextMessage(response));

            logger.info("Sent server list to user: {} available rooms", index);
        } catch (Exception e) {
            logger.error("Error handling get servers", e);
            sendError(session, "An error occurred while getting server list", "GET_SERVERS_ERROR");
        }
    }

    private void handleGetLeaderboard(WebSocketSession session, JsonObject data) {
        try {
            int limit = data.has("limit") ? data.get("limit").getAsInt() : 10;
            List<User> leaderboard = userService.getLeaderboard(limit);

            StringBuilder leadersJson = new StringBuilder("[");
            for (int i = 0; i < leaderboard.size(); i++) {
                if (i > 0) leadersJson.append(",");
                User u = leaderboard.get(i);

                // Calculate win rate
                double winRate = 0.0;
                if (u.getTotalGames() > 0) {
                    winRate = (double) u.getWins() / u.getTotalGames() * 100.0;
                }

                leadersJson.append(String.format(Locale.US,
                    "{\"rank\":%d,\"userId\":%d,\"username\":\"%s\",\"balance\":%.2f,\"points\":%d,\"wins\":%d,\"totalGames\":%d,\"winRate\":%.2f}",
                    i + 1, u.getUserId(), u.getUsername(), u.getBalance(), u.getPoints(), u.getWins(), u.getTotalGames(), winRate
                ));
            }
            leadersJson.append("]");

            String response = JsonUtils.successResponse("LEADERBOARD",
                    "Leaderboard retrieved", "{\"leaders\":" + leadersJson + "}");
            session.sendMessage(new TextMessage(response));

        } catch (Exception e) {
            logger.error("Error getting leaderboard", e);
            sendError(session, "Failed to get leaderboard", "LEADERBOARD_ERROR");
        }
    }

    private void handlePlaceBet(WebSocketSession session, JsonObject data) {
        try {
            User user = connectedClients.get(session.getId());
            if (user == null) {
                sendError(session, "User not authenticated", "NOT_AUTHENTICATED");
                return;
            }

            double amount = data.get("amount").getAsDouble();

            // Gaseste camera in care se afla utilizatorul
            String roomId = userRooms.get(user.getUserId());
            if (roomId == null) {
                sendError(session, "Not in a room", "NOT_IN_ROOM");
                return;
            }

            GameRoom room = gameRooms.get(roomId);
            if (room == null) {
                sendError(session, "Room not found", "ROOM_NOT_FOUND");
                return;
            }

            // Plaseaza pariul
            if (room.placeBet(user.getUserId(), amount)) {
                logger.info("User {} placed bet of ${} in room {}", user.getUsername(), amount, roomId);

                // Salveaza balanta actualizata in baza de date imediat
                userService.updateBalance(user.getUserId(), user.getBalance());
                logger.info("Player {} balance saved to database: ${}", user.getUsername(), user.getBalance());

                // Transmite starea actualizata a jocului catre toti jucatorii
                broadcastGameState(room);
            } else {
                sendError(session, "Failed to place bet. Check balance and bet amount.", "BET_FAILED");
            }

        } catch (Exception e) {
            logger.error("Error handling place bet", e);
            sendError(session, "An error occurred while placing bet", "BET_ERROR");
        }
    }

    private void handleHit(WebSocketSession session, JsonObject data) {
        try {
            User user = connectedClients.get(session.getId());
            if (user == null) {
                sendError(session, "User not authenticated", "NOT_AUTHENTICATED");
                return;
            }

            // Find the room the user is in
            String roomId = userRooms.get(user.getUserId());
            if (roomId == null) {
                sendError(session, "Not in a room", "NOT_IN_ROOM");
                return;
            }

            GameRoom room = gameRooms.get(roomId);
            if (room == null) {
                sendError(session, "Room not found", "ROOM_NOT_FOUND");
                return;
            }

            // Jucatorul trage o carte
            if (room.playerHit(user.getUserId())) {
                logger.info("User {} hit in room {}", user.getUsername(), roomId);
                broadcastGameState(room);
            } else {
                sendError(session, "Cannot hit at this time", "HIT_FAILED");
            }

        } catch (Exception e) {
            logger.error("Error handling hit", e);
            sendError(session, "An error occurred during hit", "HIT_ERROR");
        }
    }

    private void handleStand(WebSocketSession session, JsonObject data) {
        try {
            User user = connectedClients.get(session.getId());
            if (user == null) {
                sendError(session, "User not authenticated", "NOT_AUTHENTICATED");
                return;
            }

            // Find the room the user is in
            String roomId = userRooms.get(user.getUserId());
            if (roomId == null) {
                sendError(session, "Not in a room", "NOT_IN_ROOM");
                return;
            }

            GameRoom room = gameRooms.get(roomId);
            if (room == null) {
                sendError(session, "Room not found", "ROOM_NOT_FOUND");
                return;
            }

            // Jucatorul se opreste
            if (room.playerStand(user.getUserId())) {
                logger.info("User {} stood in room {}", user.getUsername(), roomId);
                broadcastGameState(room);
            } else {
                sendError(session, "Cannot stand at this time", "STAND_FAILED");
            }

        } catch (Exception e) {
            logger.error("Error handling stand", e);
            sendError(session, "An error occurred during stand", "STAND_ERROR");
        }
    }

    private void handleSplit(WebSocketSession session, JsonObject data) {
        try {
            User user = connectedClients.get(session.getId());
            if (user == null) {
                sendError(session, "User not authenticated", "NOT_AUTHENTICATED");
                return;
            }

            String roomId = userRooms.get(user.getUserId());
            if (roomId == null) {
                sendError(session, "Not in a room", "NOT_IN_ROOM");
                return;
            }

            GameRoom room = gameRooms.get(roomId);
            if (room == null) {
                sendError(session, "Room not found", "ROOM_NOT_FOUND");
                return;
            }

            if (room.playerSplit(user.getUserId())) {
                logger.info("User {} split hand in room {}", user.getUsername(), roomId);

                // Salveaza balanta dupa split
                userService.updateBalance(user.getUserId(), user.getBalance());

                broadcastGameState(room);
            } else {
                sendError(session, "Cannot split at this time", "SPLIT_FAILED");
            }

        } catch (Exception e) {
            logger.error("Error handling split", e);
            sendError(session, "An error occurred during split", "SPLIT_ERROR");
        }
    }

    private void handleDoubleDown(WebSocketSession session, JsonObject data) {
        try {
            User user = connectedClients.get(session.getId());
            if (user == null) {
                sendError(session, "User not authenticated", "NOT_AUTHENTICATED");
                return;
            }

            String roomId = userRooms.get(user.getUserId());
            if (roomId == null) {
                sendError(session, "Not in a room", "NOT_IN_ROOM");
                return;
            }

            GameRoom room = gameRooms.get(roomId);
            if (room == null) {
                sendError(session, "Room not found", "ROOM_NOT_FOUND");
                return;
            }

            if (room.playerDoubleDown(user.getUserId())) {
                logger.info("User {} doubled down in room {}", user.getUsername(), roomId);

                // Salveaza balanta dupa dublare
                userService.updateBalance(user.getUserId(), user.getBalance());

                broadcastGameState(room);
            } else {
                sendError(session, "Cannot double down at this time", "DOUBLE_FAILED");
            }

        } catch (Exception e) {
            logger.error("Error handling double down", e);
            sendError(session, "An error occurred during double down", "DOUBLE_ERROR");
        }
    }

    private void handleInsurance(WebSocketSession session, JsonObject data) {
        try {
            User user = connectedClients.get(session.getId());
            if (user == null) {
                sendError(session, "User not authenticated", "NOT_AUTHENTICATED");
                return;
            }

            String roomId = userRooms.get(user.getUserId());
            if (roomId == null) {
                sendError(session, "Not in a room", "NOT_IN_ROOM");
                return;
            }

            GameRoom room = gameRooms.get(roomId);
            if (room == null) {
                sendError(session, "Room not found", "ROOM_NOT_FOUND");
                return;
            }

            if (room.playerBuyInsurance(user.getUserId())) {
                logger.info("User {} bought insurance in room {}", user.getUsername(), roomId);

                // Salveaza balanta dupa cumpararea asigurarii
                userService.updateBalance(user.getUserId(), user.getBalance());

                broadcastGameState(room);
            } else {
                sendError(session, "Cannot buy insurance at this time", "INSURANCE_FAILED");
            }

        } catch (Exception e) {
            logger.error("Error handling insurance", e);
            sendError(session, "An error occurred while buying insurance", "INSURANCE_ERROR");
        }
    }

    private void handlePlayAgain(WebSocketSession session, JsonObject data) {
        try {
            User user = connectedClients.get(session.getId());
            if (user == null) {
                sendError(session, "User not authenticated", "NOT_AUTHENTICATED");
                return;
            }

            // Find the room the user is in
            String roomId = userRooms.get(user.getUserId());
            if (roomId == null) {
                sendError(session, "Not in a room", "NOT_IN_ROOM");
                return;
            }

            GameRoom room = gameRooms.get(roomId);
            if (room == null) {
                sendError(session, "Room not found", "ROOM_NOT_FOUND");
                return;
            }

            // Marcheaza jucatorul ca fiind gata pentru urmatoarea runda
            room.markPlayerReady(user.getUserId());
            logger.info("User {} is ready for next round in room {}", user.getUsername(), roomId);

            // Daca toti jucatorii sunt gata, porneste o runda noua
            if (room.allPlayersReady()) {
                logger.info("All players ready, starting new round in room {}", roomId);
                room.resetForNextRound();
                broadcastGameState(room);
            }

        } catch (Exception e) {
            logger.error("Error handling play again", e);
            sendError(session, "An error occurred", "PLAY_AGAIN_ERROR");
        }
    }

    private void handleChatMessage(WebSocketSession session, JsonObject data) {
        try {
            User user = connectedClients.get(session.getId());
            if (user == null) {
                sendError(session, "User not authenticated", "NOT_AUTHENTICATED");
                return;
            }

            String roomId = userRooms.get(user.getUserId());
            if (roomId == null) {
                sendError(session, "Not in a room", "NOT_IN_ROOM");
                return;
            }

            GameRoom room = gameRooms.get(roomId);
            if (room == null) {
                sendError(session, "Room not found", "ROOM_NOT_FOUND");
                return;
            }

            String message = data.get("message").getAsString();

            // Validare de baza a mesajului
            if (message == null || message.trim().isEmpty()) {
                sendError(session, "Empty message", "EMPTY_MESSAGE");
                return;
            }

            // Limiteaza lungimea mesajului
            if (message.length() > 200) {
                message = message.substring(0, 200);
            }

            // Creaza JSON-ul mesajului de chat
            String chatJson = String.format(java.util.Locale.US,
                "{\"userId\":%d,\"username\":\"%s\",\"message\":\"%s\",\"timestamp\":%d}",
                user.getUserId(), user.getUsername(),
                message.replace("\"", "\\\""), System.currentTimeMillis()
            );

            // Transmite catre toti jucatorii din camera
            broadcastToRoom(roomId, JsonUtils.successResponse("CHAT_MESSAGE", "Chat message",
                "{\"chat\":" + chatJson + "}"));

            logger.info("Chat message from {} in room {}: {}", user.getUsername(), roomId, message);

        } catch (Exception e) {
            logger.error("Error handling chat message", e);
            sendError(session, "An error occurred while sending chat message", "CHAT_ERROR");
        }
    }

    private void handleLeaveRoom(WebSocketSession session, int userId) {
        try {
            String roomId = userRooms.get(userId);
            if (roomId != null) {
                GameRoom room = gameRooms.get(roomId);
                if (room != null) {
                    room.removePlayer(userId);
                    logger.info("User {} left room {}", userId, roomId);

                    // Sterge din urmarire
                    userRooms.remove(userId);

                    // Transmite starea actualizata catre jucatorii ramasi
                    if (!room.isEmpty()) {
                        broadcastGameState(room);
                    } else {
                        // Reseteaza camera goala la starea WAITING pentru reutilizare
                        room.resetGame();
                        logger.info("Room {} is now empty and reset to WAITING", roomId);
                        // Pastreaza camera in gameRooms pentru potentiala reutilizare
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error handling leave room", e);
        }
    }

    // Response methods
    private void sendLoginSuccess(WebSocketSession session, User user) {
        try {
            String userData = String.format(Locale.US,
                "{\"userId\":%d,\"username\":\"%s\",\"balance\":%.2f,\"points\":%d,\"wins\":%d,\"totalGames\":%d}",
                user.getUserId(), user.getUsername(), user.getBalance(),
                user.getPoints(), user.getWins(), user.getTotalGames()
            );

            String response = JsonUtils.successResponse("LOGIN_RESPONSE", "Login successful", userData);
            session.sendMessage(new TextMessage(response));
        } catch (Exception e) {
            logger.error("Error sending login success", e);
        }
    }

    private void sendLoginError(WebSocketSession session, String message) {
        try {
            String response = JsonUtils.errorResponse(message, "LOGIN_FAILED");
            session.sendMessage(new TextMessage(response));
        } catch (Exception e) {
            logger.error("Error sending login error", e);
        }
    }

    private void sendRegisterSuccess(WebSocketSession session, User user) {
        try {
            String userData = String.format(Locale.US,
                "{\"userId\":%d,\"username\":\"%s\",\"balance\":%.2f,\"points\":%d}",
                user.getUserId(), user.getUsername(), user.getBalance(), user.getPoints()
            );

            String response = JsonUtils.successResponse("REGISTER_RESPONSE", "Registration successful", userData);
            session.sendMessage(new TextMessage(response));
        } catch (Exception e) {
            logger.error("Error sending register success", e);
        }
    }

    private void sendRegisterError(WebSocketSession session, String message) {
        try {
            String response = JsonUtils.errorResponse(message, "REGISTER_FAILED");
            session.sendMessage(new TextMessage(response));
        } catch (Exception e) {
            logger.error("Error sending register error", e);
        }
    }

    private void sendError(WebSocketSession session, String message, String code) {
        try {
            String error = JsonUtils.errorResponse(message, code);
            session.sendMessage(new TextMessage(error));
        } catch (Exception e) {
            logger.error("Error sending error message", e);
        }
    }

    /**
     * Transmite un mesaj catre toti jucatorii dintr-o camera specifica
     */
    public void broadcastToRoom(String roomId, String message) {
        GameRoom room = gameRooms.get(roomId);
        if (room != null) {
            // TODO: Implementare transmitere catre toti jucatorii din camera
            logger.debug("Broadcasting to room {}: {}", roomId, message);
        }
    }

    /**
     * Obtine statistici server
     */
    public String getServerStats() {
        int totalPlayers = connectedClients.size();
        int totalRooms = gameRooms.size();
        int activeGames = (int) gameRooms.values().stream()
            .filter(room -> room.getCurrentPhase() != GameRoom.GamePhase.WAITING)
            .count();

        return String.format("Server Stats - Players: %d, Rooms: %d, Active Games: %d",
            totalPlayers, totalRooms, activeGames);
    }
}
