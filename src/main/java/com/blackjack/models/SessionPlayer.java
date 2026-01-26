package com.blackjack.models;

import jakarta.persistence.*;

@Entity
@Table(name = "session_players")
public class SessionPlayer {

    public enum GameResult {
        win, lose, push
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession gameSession;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "bet_amount", columnDefinition = "DECIMAL(10,2)")
    private Double betAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", columnDefinition = "game_result DEFAULT 'lose'")
    private GameResult result;

    @Column(name = "winnings", columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    private double winnings;

    // Constructors
    public SessionPlayer() {
        this.result = GameResult.lose;
        this.winnings = 0.00;
    }

    public SessionPlayer(GameSession gameSession, int userId, Double betAmount) {
        this.gameSession = gameSession;
        this.userId = userId;
        this.betAmount = betAmount;
        this.result = GameResult.lose;
        this.winnings = 0.00;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public GameSession getGameSession() {
        return gameSession;
    }

    public void setGameSession(GameSession gameSession) {
        this.gameSession = gameSession;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Double getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(Double betAmount) {
        this.betAmount = betAmount;
    }

    public GameResult getResult() {
        return result;
    }

    public void setResult(GameResult result) {
        this.result = result;
    }

    public double getWinnings() {
        return winnings;
    }

    public void setWinnings(double winnings) {
        this.winnings = winnings;
    }

    @Override
    public String toString() {
        return String.format("SessionPlayer{id=%d, userId=%d, bet=%.2f, result=%s, winnings=%.2f}",
                id, userId, betAmount, result, winnings);
    }
}
