package com.blackjack.models;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "game_sessions")
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private int sessionId;

    @Column(name = "room_id", nullable = false, length = 50)
    private String roomId;

    @CreationTimestamp
    @Column(name = "start_time", updatable = false)
    private Timestamp startTime;

    @Column(name = "end_time")
    private Timestamp endTime;

    @Column(name = "total_pot", columnDefinition = "DECIMAL(10,2)")
    private Double totalPot;

    @Column(name = "winner_id")
    private Integer winnerId;

    @OneToMany(mappedBy = "gameSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessionPlayer> sessionPlayers = new ArrayList<>();

    // Constructors
    public GameSession() {
    }

    public GameSession(String roomId) {
        this.roomId = roomId;
    }

    // Getters and Setters
    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public Double getTotalPot() {
        return totalPot;
    }

    public void setTotalPot(Double totalPot) {
        this.totalPot = totalPot;
    }

    public Integer getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(Integer winnerId) {
        this.winnerId = winnerId;
    }

    public List<SessionPlayer> getSessionPlayers() {
        return sessionPlayers;
    }

    public void setSessionPlayers(List<SessionPlayer> sessionPlayers) {
        this.sessionPlayers = sessionPlayers;
    }

    // Utility methods
    public void addSessionPlayer(SessionPlayer sessionPlayer) {
        sessionPlayers.add(sessionPlayer);
        sessionPlayer.setGameSession(this);
    }

    public void removeSessionPlayer(SessionPlayer sessionPlayer) {
        sessionPlayers.remove(sessionPlayer);
        sessionPlayer.setGameSession(null);
    }

    @Override
    public String toString() {
        return String.format("GameSession{id=%d, roomId='%s', pot=%.2f}",
                sessionId, roomId, totalPot);
    }
}
