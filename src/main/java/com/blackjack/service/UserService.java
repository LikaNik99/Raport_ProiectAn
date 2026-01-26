package com.blackjack.service;

import com.blackjack.models.User;
import com.blackjack.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Actualizeaza balanta utilizatorului
     */
    @Transactional
    public boolean updateBalance(int userId, double balance) {
        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isEmpty()) {
            logger.error("User not found: {}", userId);
            return false;
        }

        User user = userOptional.get();
        user.setBalance(balance);
        userRepository.save(user);
        return true;
    }

    /**
     * Actualizeaza punctele utilizatorului
     */
    @Transactional
    public boolean updatePoints(int userId, int points) {
        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isEmpty()) {
            logger.error("User not found: {}", userId);
            return false;
        }

        User user = userOptional.get();
        user.setPoints(points);
        userRepository.save(user);
        return true;
    }

    /**
     * Inregistreaza rezultatul jocului (castig sau pierdere)
     */
    @Transactional
    public boolean recordGameResult(int userId, boolean won, double balanceChange) {
        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isEmpty()) {
            logger.error("User not found: {}", userId);
            return false;
        }

        User user = userOptional.get();

        // Actualizeaza balanta
        user.setBalance(user.getBalance() + balanceChange);

        // Actualizeaza castiguri/pierderi si total jocuri
        if (won) {
            user.setWins(user.getWins() + 1);
            int pointsEarned = (int) (Math.abs(balanceChange) / 10);
            user.setPoints(user.getPoints() + pointsEarned);
        } else {
            user.setLosses(user.getLosses() + 1);
        }

        user.setTotalGames(user.getTotalGames() + 1);

        userRepository.save(user);
        return true;
    }

    /**
     * Actualizeaza statisticile utilizatorului dupa un joc
     */
    @Transactional
    public boolean updateUserStats(User user) {
        // Preia utilizatorul din baza de date pentru a lucra cu o entitate gestionata
        Optional<User> dbUserOptional = userRepository.findById(user.getUserId());

        if (dbUserOptional.isEmpty()) {
            logger.error("User not found when updating stats: {}", user.getUserId());
            return false;
        }

        User dbUser = dbUserOptional.get();

        // Actualizeaza toate campurile din utilizatorul in memorie
        dbUser.setBalance(user.getBalance());
        dbUser.setPoints(user.getPoints());
        dbUser.setWins(user.getWins());
        dbUser.setLosses(user.getLosses());
        dbUser.setTotalGames(user.getTotalGames());

        userRepository.save(dbUser);
        logger.debug("Updated stats for user {}: balance={}, points={}, wins={}, losses={}, totalGames={}",
            dbUser.getUsername(), dbUser.getBalance(), dbUser.getPoints(),
            dbUser.getWins(), dbUser.getLosses(), dbUser.getTotalGames());

        return true;
    }

    /**
     * Obtine cei mai buni jucatori dupa puncte (pentru clasament)
     */
    public List<User> getLeaderboard(int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        return userRepository.findAll(pageRequest).getContent().stream()
            .sorted((u1, u2) -> Integer.compare(u2.getPoints(), u1.getPoints()))
            .limit(limit)
            .toList();
    }

    /**
     * Obtine rangul utilizatorului in clasament
     */
    public int getUserRank(int userId) {
        return userRepository.getUserRank(userId);
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
