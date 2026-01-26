package com.blackjack.service;

import com.blackjack.models.GameSession;
import com.blackjack.models.SessionPlayer;
import com.blackjack.repository.GameSessionRepository;
import com.blackjack.repository.SessionPlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Service
public class GameSessionService {

    private static final Logger logger = LoggerFactory.getLogger(GameSessionService.class);

    private final GameSessionRepository gameSessionRepository;
    private final SessionPlayerRepository sessionPlayerRepository;

    @Autowired
    public GameSessionService(GameSessionRepository gameSessionRepository,
                              SessionPlayerRepository sessionPlayerRepository) {
        this.gameSessionRepository = gameSessionRepository;
        this.sessionPlayerRepository = sessionPlayerRepository;
    }

    /**
     * Creaza o sesiune de joc noua
     * Returneaza GameSession creat
     */
    @Transactional
    public GameSession createSession(String roomId) {
        GameSession session = new GameSession(roomId);
        GameSession savedSession = gameSessionRepository.save(session);
        logger.info("Created game session: {} for room: {}", savedSession.getSessionId(), roomId);
        return savedSession;
    }

    /**
     * Incheie o sesiune de joc cu informatii despre castigator si pot
     */
    @Transactional
    public boolean endSession(int sessionId, Integer winnerId, double totalPot) {
        Optional<GameSession> sessionOptional = gameSessionRepository.findById(sessionId);

        if (sessionOptional.isEmpty()) {
            logger.error("Game session not found: {}", sessionId);
            return false;
        }

        GameSession session = sessionOptional.get();
        session.setEndTime(new Timestamp(System.currentTimeMillis()));
        session.setWinnerId(winnerId);
        session.setTotalPot(totalPot);

        gameSessionRepository.save(session);
        logger.info("Ended game session: {}", sessionId);
        return true;
    }

    /**
     * Adauga un jucator la o sesiune cu pariul si rezultatul sau
     */
    @Transactional
    public boolean addPlayerToSession(int sessionId, int userId, double betAmount,
                                      SessionPlayer.GameResult result, double winnings) {
        Optional<GameSession> sessionOptional = gameSessionRepository.findById(sessionId);

        if (sessionOptional.isEmpty()) {
            logger.error("Game session not found: {}", sessionId);
            return false;
        }

        GameSession session = sessionOptional.get();
        SessionPlayer sessionPlayer = new SessionPlayer(session, userId, betAmount);
        sessionPlayer.setResult(result);
        sessionPlayer.setWinnings(winnings);

        sessionPlayerRepository.save(sessionPlayer);
        return true;
    }

    /**
     * Obtine numarul total de sesiuni incheiate
     */
    public long getTotalSessions() {
        return gameSessionRepository.countCompletedSessions();
    }

    /**
     * Obtine potul total distribuit
     */
    public double getTotalPotDistributed() {
        return gameSessionRepository.getTotalPotDistributed();
    }

    /**
     * Gaseste sesiunea dupa ID camera
     */
    public Optional<GameSession> findByRoomId(String roomId) {
        return gameSessionRepository.findByRoomId(roomId);
    }

    /**
     * Obtine toate sesiunile active
     */
    public List<GameSession> getActiveSessions() {
        return gameSessionRepository.findByEndTimeIsNull();
    }
}
