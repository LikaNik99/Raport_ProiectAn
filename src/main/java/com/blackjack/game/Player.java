package com.blackjack.game;

import com.blackjack.models.Card;
import com.blackjack.models.User;
import org.java_websocket.WebSocket;

public class Player {
    public enum PlayerStatus {
        WAITING,
        BETTING,
        PLAYING,
        STANDING,
        BUSTED,
        BLACKJACK,
        WON,
        LOST,
        PUSH
    }

    private User user;
    private WebSocket connection;
    private Hand hand;
    private Hand splitHand;  // A doua mana pentru impartire
    private int activeHandIndex; // 0 = mana principala, 1 = mana impartita
    private double currentBet;
    private double insuranceBet;
    private PlayerStatus status;
    private boolean hasBet;
    private boolean hasDoubledDown;
    private boolean hasSplit;

    public Player(User user, WebSocket connection) {
        this.user = user;
        this.connection = connection;
        this.hand = new Hand();
        this.splitHand = null;
        this.activeHandIndex = 0;
        this.currentBet = 0;
        this.insuranceBet = 0;
        this.status = PlayerStatus.WAITING;
        this.hasBet = false;
        this.hasDoubledDown = false;
        this.hasSplit = false;
    }

    // Getteri
    public User getUser() {
        return user;
    }

    public WebSocket getConnection() {
        return connection;
    }

    public Hand getHand() {
        return hand;
    }

    public double getCurrentBet() {
        return currentBet;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public boolean hasBet() {
        return hasBet;
    }

    public int getUserId() {
        return user.getUserId();
    }

    public String getUsername() {
        return user.getUsername();
    }

    public double getBalance() {
        return user.getBalance();
    }

    public Hand getSplitHand() {
        return splitHand;
    }

    public int getActiveHandIndex() {
        return activeHandIndex;
    }

    public double getInsuranceBet() {
        return insuranceBet;
    }

    public boolean hasDoubledDown() {
        return hasDoubledDown;
    }

    public boolean hasSplit() {
        return hasSplit;
    }

    public Hand getActiveHand() {
        if (hasSplit && activeHandIndex == 1 && splitHand != null) {
            return splitHand;
        }
        return hand;
    }

    // Setteri
    public void setStatus(PlayerStatus status) {
        this.status = status;
    }

    public void setConnection(WebSocket connection) {
        this.connection = connection;
    }

    // Actiuni de joc
    public boolean placeBet(double amount) {
        if (user.deductBalance(amount)) {
            this.currentBet = amount;
            this.hasBet = true;
            this.status = PlayerStatus.BETTING;
            return true;
        }
        return false;
    }

    public void addCard(Card card) {
        // Adauga la mana activa
        Hand currentHand = getActiveHand();
        currentHand.addCard(card);

        // Actualizeaza statusul bazat pe valoarea mainii
        if (currentHand.isBlackjack()) {
            status = PlayerStatus.BLACKJACK;
        } else if (currentHand.isBust()) {
            status = PlayerStatus.BUSTED;
        }

        // Daca s-a dublat, stand automat dupa primirea cartii
        if (hasDoubledDown) {
            stand();
        }
    }

    public void hit(Card card) {
        addCard(card);
    }

    public void stand() {
        this.status = PlayerStatus.STANDING;
    }

    public int getHandValue() {
        return hand.getValue();
    }

    public boolean isBusted() {
        return hand.isBust();
    }

    public boolean hasBlackjack() {
        return hand.isBlackjack();
    }

    public void winBet(double winnings) {
        user.addBalance(currentBet + winnings);
        user.addPoints((int)(winnings / 10)); // 1 punct per 10 jetoane castigate
        user.incrementWins();
        status = PlayerStatus.WON;
    }

    public void loseBet() {
        user.incrementLosses();
        status = PlayerStatus.LOST;
    }

    public void pushBet() {
        user.addBalance(currentBet); // Return the bet
        status = PlayerStatus.PUSH;
    }

    /**
     * Imparte mana jucatorului in doua maini separate
     */
    public boolean split() {
        if (!hand.canSplit() || hasSplit) {
            return false;
        }

        // Deduce pariul aditional pentru mana impartita
        if (!user.deductBalance(currentBet)) {
            return false;
        }

        // Creaza mana impartita si muta a doua carte
        Card secondCard = hand.splitHand();
        if (secondCard == null) {
            user.addBalance(currentBet); // Returneaza daca impartirea a esuat
            return false;
        }

        splitHand = new Hand();
        splitHand.addCard(secondCard);
        hasSplit = true;
        activeHandIndex = 0; // Incepe cu prima mana

        return true;
    }

    /**
     * Dubleaza - dubleaza pariul si primeste inca o carte
     */
    public boolean doubleDown() {
        if (hasDoubledDown || hand.getCardCount() != 2 || hasSplit) {
            return false;
        }

        // Deduce pariul aditional
        if (!user.deductBalance(currentBet)) {
            return false;
        }

        currentBet *= 2; // Dubleaza pariul
        hasDoubledDown = true;

        return true;
    }

    /**
     * Cumpara asigurare cand dealerul arata As
     */
    public boolean buyInsurance() {
        if (insuranceBet > 0 || hand.getCardCount() != 2) {
            return false;
        }

        double insuranceAmount = currentBet / 2.0;
        if (!user.deductBalance(insuranceAmount)) {
            return false;
        }

        insuranceBet = insuranceAmount;
        return true;
    }

    /**
     * Plateste pariul de asigurare daca dealerul are blackjack
     */
    public void payInsurance() {
        if (insuranceBet > 0) {
            user.addBalance(insuranceBet * 3); // Plata 2:1 plus pariul original de asigurare
        }
    }

    /**
     * Muta la urmatoarea mana (pentru mainile impartite)
     */
    public void nextHand() {
        if (hasSplit && activeHandIndex == 0) {
            activeHandIndex = 1;
        }
    }

    /**
     * Verifica daca jucatorul are mai multe maini de jucat
     */
    public boolean hasMoreHands() {
        return hasSplit && activeHandIndex == 0;
    }

    public void resetForNewRound() {
        hand.clear();
        splitHand = null;
        activeHandIndex = 0;
        currentBet = 0;
        insuranceBet = 0;
        hasBet = false;
        hasDoubledDown = false;
        hasSplit = false;
        status = PlayerStatus.WAITING;
    }

    public boolean isConnected() {
        return connection != null && connection.isOpen();
    }

    @Override
    public String toString() {
        return String.format("Player{username='%s', bet=%.2f, hand=%s, status=%s}",
                user.getUsername(), currentBet, hand, status);
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append(String.format(java.util.Locale.US,
            "{\"userId\":%d,\"username\":\"%s\",\"balance\":%.2f,\"bet\":%.2f,\"hand\":%s,\"handValue\":%d,\"status\":\"%s\"",
            user.getUserId(), user.getUsername(), user.getBalance(), currentBet,
            hand.toJson(), hand.getValue(), status.toString().toLowerCase()
        ));

        // Adauga mana impartita daca exista
        if (hasSplit && splitHand != null) {
            json.append(String.format(java.util.Locale.US,
                ",\"splitHand\":%s,\"splitHandValue\":%d,\"activeHandIndex\":%d",
                splitHand.toJson(), splitHand.getValue(), activeHandIndex
            ));
        }

        // Adauga asigurare daca exista
        if (insuranceBet > 0) {
            json.append(String.format(java.util.Locale.US, ",\"insuranceBet\":%.2f", insuranceBet));
        }

        // Adauga marcaje
        json.append(String.format(",\"hasDoubledDown\":%b,\"hasSplit\":%b", hasDoubledDown, hasSplit));

        json.append("}");
        return json.toString();
    }
}
