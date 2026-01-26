package com.blackjack.models;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private int userId;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "balance", columnDefinition = "DECIMAL(10,2) DEFAULT 1000.00")
    private double balance;

    @Column(name = "points", columnDefinition = "INT DEFAULT 0")
    private int points;

    @Column(name = "total_games", columnDefinition = "INT DEFAULT 0")
    private int totalGames;

    @Column(name = "wins", columnDefinition = "INT DEFAULT 0")
    private int wins;

    @Column(name = "losses", columnDefinition = "INT DEFAULT 0")
    private int losses;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    public User() {
    }

    public User(int userId, String username, String passwordHash, double balance, int points) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.balance = balance;
        this.points = points;
    }

    // Getters
    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public double getBalance() {
        return balance;
    }

    public int getPoints() {
        return points;
    }

    public int getTotalGames() {
        return totalGames;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void setTotalGames(int totalGames) {
        this.totalGames = totalGames;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // Utility methods
    public void addBalance(double amount) {
        this.balance += amount;
    }

    public boolean deductBalance(double amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

    public void addPoints(int points) {
        this.points += points;
    }

    public void incrementWins() {
        this.wins++;
        this.totalGames++;
    }

    public void incrementLosses() {
        this.losses++;
        this.totalGames++;
    }

    public double getWinRate() {
        if (totalGames == 0) return 0.0;
        return (double) wins / totalGames * 100;
    }

    @Override
    public String toString() {
        return String.format("User{id=%d, username='%s', balance=%.2f, points=%d}",
                userId, username, balance, points);
    }
}
