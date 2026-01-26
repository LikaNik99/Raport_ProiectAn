package com.blackjack.repository;

import com.blackjack.models.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Integer> {

    /**
     * Gaseste sesiunea de joc dupa ID camera
     */
    Optional<GameSession> findByRoomId(String roomId);

    /**
     * Gaseste toate sesiunile active (neincheiate inca)
     */
    List<GameSession> findByEndTimeIsNull();

    /**
     * Gaseste toate sesiunile incheiate
     */
    List<GameSession> findByEndTimeIsNotNull();

    /**
     * Numara sesiunile incheiate total
     */
    @Query("SELECT COUNT(gs) FROM GameSession gs WHERE gs.endTime IS NOT NULL")
    long countCompletedSessions();

    /**
     * Obtine potul total distribuit in toate sesiunile incheiate
     */
    @Query("SELECT COALESCE(SUM(gs.totalPot), 0.0) FROM GameSession gs WHERE gs.endTime IS NOT NULL")
    double getTotalPotDistributed();

    /**
     * Gaseste sesiunile dupa ID castigator
     */
    List<GameSession> findByWinnerId(Integer winnerId);
}
