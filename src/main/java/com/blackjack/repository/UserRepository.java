package com.blackjack.repository;

import com.blackjack.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Gaseste utilizatorul dupa nume utilizator
     */
    Optional<User> findByUsername(String username);

    /**
     * Verifica daca numele de utilizator exista
     */
    boolean existsByUsername(String username);

    /**
     * Obtine cei mai buni utilizatori dupa puncte (pentru clasament)
     */
    List<User> findTopByOrderByPointsDesc();

    /**
     * Obtine clasamentul cu limita
     */
    @Query("SELECT u FROM User u ORDER BY u.points DESC")
    List<User> findLeaderboard(@Param("limit") int limit);

    /**
     * Obtine rangul utilizatorului dupa puncte
     */
    @Query("SELECT COUNT(u) + 1 FROM User u WHERE u.points > " +
           "(SELECT u2.points FROM User u2 WHERE u2.userId = :userId)")
    int getUserRank(@Param("userId") int userId);

    /**
     * Gaseste utilizatorii cu balanta mai mare decat suma
     */
    List<User> findByBalanceGreaterThanEqual(double balance);
}
