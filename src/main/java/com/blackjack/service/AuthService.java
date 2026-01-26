package com.blackjack.service;

import com.blackjack.models.User;
import com.blackjack.repository.UserRepository;
import com.blackjack.utils.JsonUtils;
import com.blackjack.utils.PasswordUtils;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;

    @Autowired
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Gestioneaza cererea de autentificare
     */
    public User handleLogin(String username, String password) {
        logger.info("Login attempt for user: {}", username);

        // Valideaza intrarea
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("Username and password are required");
        }

        // Obtine utilizatorul din baza de date
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        User user = userOptional.get();

        // Verifica parola
        if (!PasswordUtils.checkPassword(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        logger.info("User logged in successfully: {}", username);
        return user;
    }

    /**
     * Gestioneaza cererea de inregistrare
     */
    @Transactional
    public User handleRegister(String username, String password) {
        logger.info("Registration attempt for user: {}", username);

        // Valideaza numele de utilizator
        if (!PasswordUtils.isValidUsername(username)) {
            throw new IllegalArgumentException("Username must be 3-20 characters, alphanumeric and underscores only");
        }

        // Valideaza parola
        if (!PasswordUtils.isValidPassword(password)) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        // Verifica daca numele de utilizator exista deja
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken");
        }

        // Hash-uieste parola si creaza utilizatorul
        String passwordHash = PasswordUtils.hashPassword(password);

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPasswordHash(passwordHash);
        newUser.setBalance(1000.00);  // Balanta initiala
        newUser.setPoints(0);
        newUser.setTotalGames(0);
        newUser.setWins(0);
        newUser.setLosses(0);

        User savedUser = userRepository.save(newUser);
        logger.info("User registered successfully: {}", username);

        return savedUser;
    }

    /**
     * Obtine utilizatorul dupa ID
     */
    public Optional<User> getUserById(int userId) {
        return userRepository.findById(userId);
    }

    /**
     * Obtine utilizatorul dupa nume utilizator
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
