package com.blackjack.handlers;

import com.blackjack.database.UserDAO;
import com.blackjack.models.User;
import com.blackjack.utils.JsonUtils;
import com.blackjack.utils.PasswordUtils;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);
    private UserDAO userDAO;

    public AuthHandler() {
        this.userDAO = new UserDAO();
    }

    /**
     * Gestioneaza cererea de autentificare
     */
    public void handleLogin(WebSocket conn, JsonObject data) {
        try {
            String username = data.get("username").getAsString();
            String password = data.get("password").getAsString();

            logger.info("Login attempt for user: {}", username);

            // Valideaza intrarea
            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                sendLoginError(conn, "Username and password are required");
                return;
            }

            // Obtine utilizatorul din baza de date
            User user = userDAO.getUserByUsername(username);

            if (user == null) {
                sendLoginError(conn, "Invalid username or password");
                return;
            }

            // Verifica parola
            if (!PasswordUtils.checkPassword(password, user.getPasswordHash())) {
                sendLoginError(conn, "Invalid username or password");
                return;
            }

            // Trimite raspuns de succes
            sendLoginSuccess(conn, user);
            logger.info("User logged in successfully: {}", username);

        } catch (Exception e) {
            logger.error("Error handling login", e);
            sendLoginError(conn, "An error occurred during login");
        }
    }

    /**
     * Gestioneaza cererea de inregistrare
     */
    public void handleRegister(WebSocket conn, JsonObject data) {
        try {
            String username = data.get("username").getAsString();
            String password = data.get("password").getAsString();

            logger.info("Registration attempt for user: {}", username);

            // Valideaza numele de utilizator
            if (!PasswordUtils.isValidUsername(username)) {
                sendRegisterError(conn, "Username must be 3-20 characters, alphanumeric and underscores only");
                return;
            }

            // Valideaza parola
            if (!PasswordUtils.isValidPassword(password)) {
                sendRegisterError(conn, "Password must be at least 6 characters");
                return;
            }

            // Verifica daca numele de utilizator exista deja
            if (userDAO.usernameExists(username)) {
                sendRegisterError(conn, "Username already taken");
                return;
            }

            // Hash-uieste parola si creaza utilizatorul
            String passwordHash = PasswordUtils.hashPassword(password);
            boolean created = userDAO.createUser(username, passwordHash);

            if (!created) {
                sendRegisterError(conn, "Failed to create user");
                return;
            }

            // Obtine utilizatorul creat
            User user = userDAO.getUserByUsername(username);
            if (user != null) {
                sendRegisterSuccess(conn, user);
                logger.info("User registered successfully: {}", username);
            } else {
                sendRegisterError(conn, "Failed to retrieve user after creation");
            }

        } catch (Exception e) {
            logger.error("Error handling registration", e);
            sendRegisterError(conn, "An error occurred during registration");
        }
    }

    private void sendLoginSuccess(WebSocket conn, User user) {
        String userData = String.format(
            "{\"userId\":%d,\"username\":\"%s\",\"balance\":%.2f,\"points\":%d,\"wins\":%d,\"totalGames\":%d}",
            user.getUserId(), user.getUsername(), user.getBalance(),
            user.getPoints(), user.getWins(), user.getTotalGames()
        );

        String response = JsonUtils.successResponse("LOGIN_RESPONSE", "Login successful", userData);
        conn.send(response);
    }

    private void sendLoginError(WebSocket conn, String message) {
        String response = JsonUtils.errorResponse(message, "LOGIN_FAILED");
        conn.send(response);
    }

    private void sendRegisterSuccess(WebSocket conn, User user) {
        String userData = String.format(
            "{\"userId\":%d,\"username\":\"%s\",\"balance\":%.2f,\"points\":%d}",
            user.getUserId(), user.getUsername(), user.getBalance(), user.getPoints()
        );

        String response = JsonUtils.successResponse("REGISTER_RESPONSE", "Registration successful", userData);
        conn.send(response);
    }

    private void sendRegisterError(WebSocket conn, String message) {
        String response = JsonUtils.errorResponse(message, "REGISTER_FAILED");
        conn.send(response);
    }
}
