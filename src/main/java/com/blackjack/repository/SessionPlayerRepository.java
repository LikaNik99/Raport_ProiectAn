package com.blackjack.repository;

import com.blackjack.models.SessionPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionPlayerRepository extends JpaRepository<SessionPlayer, Integer> {

    /**
     * Gaseste toti jucatorii sesiunii dupa ID utilizator
     */
    List<SessionPlayer> findByUserId(int userId);

    /**
     * Gaseste toti jucatorii sesiunii dupa ID sesiune joc
     */
    List<SessionPlayer> findByGameSession_SessionId(int sessionId);

    /**
     * Gaseste toti jucatorii sesiunii dupa rezultat
     */
    List<SessionPlayer> findByResult(SessionPlayer.GameResult result);
}
