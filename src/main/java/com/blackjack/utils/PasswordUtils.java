package com.blackjack.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordUtils {

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    /**
     * Hash-uieste o parola folosind BCrypt
     */
    public static String hashPassword(String plainTextPassword) {
        return passwordEncoder.encode(plainTextPassword);
    }

    /**
     * Verifica daca o parola in text clar se potriveste cu o parola hash-uita
     */
    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        try {
            return passwordEncoder.matches(plainTextPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Valideaza cerintele parolei
     * - Minim 6 caractere
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }
        return true;
    }

    /**
     * Valideaza cerintele numelui de utilizator
     * - 3-20 caractere
     * - Doar alfanumeric si liniute jos
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.length() < 3 || username.length() > 20) {
            return false;
        }
        return username.matches("^[a-zA-Z0-9_]+$");
    }
}
