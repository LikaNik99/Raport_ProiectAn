package com.blackjack.server;

import com.blackjack.database.DatabaseManager;
import com.blackjack.game.GameRoom;
import com.blackjack.handlers.AuthHandler;
import com.blackjack.handlers.GameHandler;
import com.blackjack.handlers.LobbyHandler;
import com.blackjack.models.User;
import com.blackjack.utils.JsonUtils;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlackjackServer extends WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(BlackjackServer.class);

    private Map<String, GameRoom> gameRooms;
    private Map<WebSocket, User> connectedClients;
    private Queue<User> quickMatchQueue;

    private AuthHandler authHandler;
    private GameHandler gameHandler;
    private LobbyHandler lobbyHandler;

    public BlackjackServer(int port) {
        super(new InetSocketAddress(port));
        this.gameRooms = new ConcurrentHashMap<>();
        this.connectedClients = new ConcurrentHashMap<>();
        this.quickMatchQueue = new LinkedList<>();

        this.authHandler = new AuthHandler();
        this.gameHandler = new GameHandler(gameRooms);
        this.lobbyHandler = new LobbyHandler(gameRooms, quickMatchQueue);

        logger.info("Blackjack Server initialized on port {}", port);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.info("New connection from: {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        logger.info("Connection closed: {} - Reason: {}", conn.getRemoteSocketAddress(), reason);

        // Gaseste si sterge utilizatorul din jocurile active
        User user = connectedClients.get(conn);
        if (user != null) {
            lobbyHandler.handleLeaveRoom(user.getUserId());
            connectedClients.remove(conn);
            logger.info("User {} disconnected", user.getUsername());
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            logger.debug("Received message: {}", message);

            JsonObject json = JsonUtils.parse(message);
            String type = json.get("type").getAsString();
            JsonObject data = json.has("data") ? json.getAsJsonObject("data") : new JsonObject();

            // Rutare mesaj catre gestionarul corespunzator
            switch (type) {
                case "LOGIN":
                    authHandler.handleLogin(conn, data);
                    break;

                case "REGISTER":
                    authHandler.handleRegister(conn, data);
                    break;

                case "QUICK_MATCH":
                    lobbyHandler.handleQuickMatch(conn, data);
                    break;

                case "JOIN_ROOM":
                    lobbyHandler.handleJoinRoom(conn, data);
                    break;

                case "GET_SERVERS":
                    lobbyHandler.handleGetServers(conn);
                    break;

                case "GET_LEADERBOARD":
                    lobbyHandler.handleGetLeaderboard(conn, data);
                    break;

                case "PLACE_BET":
                    gameHandler.handlePlaceBet(conn, data);
                    break;

                case "HIT":
                    gameHandler.handleHit(conn, data);
                    break;

                case "STAND":
                    gameHandler.handleStand(conn, data);
                    break;

                case "LEAVE_ROOM":
                    int userId = data.get("userId").getAsInt();
                    lobbyHandler.handleLeaveRoom(userId);
                    break;

                default:
                    logger.warn("Unknown message type: {}", type);
                    sendError(conn, "Unknown message type", "UNKNOWN_TYPE");
            }

        } catch (Exception e) {
            logger.error("Error processing message", e);
            sendError(conn, "Error processing message", "MESSAGE_ERROR");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        logger.error("WebSocket error", ex);
        if (conn != null) {
            logger.error("Error on connection: {}", conn.getRemoteSocketAddress());
        }
    }

    @Override
    public void onStart() {
        logger.info("Blackjack Server started successfully!");
        logger.info("Listening on port: {}", getPort());

        // Testeaza conexiunea la baza de date
        DatabaseManager dbManager = DatabaseManager.getInstance();
        if (dbManager.testConnection()) {
            logger.info("Database connection successful");
        } else {
            logger.error("Database connection failed!");
        }
    }

    /**
     * Transmite un mesaj catre toti jucatorii dintr-o camera specifica
     */
    public void broadcastToRoom(String roomId, String message) {
        GameRoom room = gameRooms.get(roomId);
        if (room != null) {
            room.getPlayers().forEach(player -> {
                if (player.isConnected()) {
                    player.getConnection().send(message);
                }
            });
        }
    }

    /**
     * Transmite un mesaj catre toti clientii conectati
     */
    public void broadcastToAll(String message) {
        broadcast(message);
    }

    /**
     * Trimite mesaj de eroare catre un client specific
     */
    private void sendError(WebSocket conn, String message, String code) {
        String error = JsonUtils.errorResponse(message, code);
        conn.send(error);
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

    /**
     * Metoda principala pentru a porni serverul
     */
    public static void main(String[] args) {
        int port = 8080;

        // Verifica daca este furnizat un port personalizat
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.error("Invalid port number, using default: 8080");
            }
        }

        BlackjackServer server = new BlackjackServer(port);
        server.start();

        // Adauga hook de inchidere
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down server...");
            try {
                server.stop(1000);
                DatabaseManager.getInstance().closeDataSource();
                logger.info("Server stopped successfully");
            } catch (InterruptedException e) {
                logger.error("Error stopping server", e);
            }
        }));

        // Afiseaza statistici la fiecare 60 de secunde
        Timer statsTimer = new Timer(true);
        statsTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                logger.info(server.getServerStats());
            }
        }, 60000, 60000);
    }
}
