package com.blackjack.game;

import com.blackjack.models.Card;
import com.blackjack.models.User;
import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GameRoom {
    public enum GamePhase {
        WAITING,
        BETTING,
        DEALING,
        PLAYER_TURNS,
        DEALER_TURN,
        RESULTS,
        FINISHED
    }

    private String roomId;
    private List<Player> players;
    private Dealer dealer;
    private GamePhase currentPhase;
    private int currentPlayerIndex;
    private double pot;
    private final int maxPlayers;
    private final double minBet;
    private int sessionId;
    private Set<Integer> readyPlayers;
    private String serverName;
    private String password;

    public GameRoom(int maxPlayers, double minBet) {
        this.roomId = "room_" + UUID.randomUUID().toString().substring(0, 8);
        this.players = new ArrayList<>();
        this.dealer = new Dealer();
        this.currentPhase = GamePhase.WAITING;
        this.currentPlayerIndex = 0;
        this.pot = 0;
        this.maxPlayers = maxPlayers;
        this.minBet = minBet;
        this.sessionId = -1;
        this.readyPlayers = new HashSet<>();
        this.serverName = "";
        this.password = "";
    }

    public GameRoom(int maxPlayers, double minBet, String serverName, String password) {
        this.roomId = "room_" + UUID.randomUUID().toString().substring(0, 8);
        this.players = new ArrayList<>();
        this.dealer = new Dealer();
        this.currentPhase = GamePhase.WAITING;
        this.currentPlayerIndex = 0;
        this.pot = 0;
        this.maxPlayers = maxPlayers;
        this.minBet = minBet;
        this.sessionId = -1;
        this.readyPlayers = new HashSet<>();
        this.serverName = serverName != null ? serverName : "";
        this.password = password != null ? password : "";
    }

    // Getteri
    public String getRoomId() {
        return roomId;
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public Dealer getDealer() {
        return dealer;
    }

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    public double getPot() {
        return pot;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public double getMinBet() {
        return minBet;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public String getServerName() {
        return serverName;
    }

    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }

    public boolean checkPassword(String inputPassword) {
        if (!hasPassword()) {
            return true; // Nu este necesara parola
        }
        return password.equals(inputPassword);
    }

    // Gestionarea jucatorilor
    public boolean addPlayer(User user, WebSocket connection) {
        // Permite intrarea in timpul fazei WAITING sau BETTING (inainte ca cartile sa fie impartite)
        if (isFull() ||
            (currentPhase != GamePhase.WAITING && currentPhase != GamePhase.BETTING)) {
            return false;
        }

        Player player = new Player(user, connection);
        players.add(player);

        // Porneste jocul daca avem cel putin 1 jucator si vor sa inceapa
        // (In implementarea reala, ar putea astepta jucatori minimi sau semnal de start)

        return true;
    }

    public boolean removePlayer(int userId) {
        Player player = findPlayerByUserId(userId);
        if (player != null) {
            // Daca jucatorul a pariat si jocul este in desfasurare, gestioneaza returnarea
            if (player.hasBet() && currentPhase != GamePhase.WAITING) {
                player.getUser().addBalance(player.getCurrentBet());
                pot -= player.getCurrentBet();
            }
            players.remove(player);
            return true;
        }
        return false;
    }

    public Player findPlayerByUserId(int userId) {
        for (Player player : players) {
            if (player.getUserId() == userId) {
                return player;
            }
        }
        return null;
    }

    public Player getCurrentPlayer() {
        if (currentPlayerIndex >= 0 && currentPlayerIndex < players.size()) {
            return players.get(currentPlayerIndex);
        }
        return null;
    }

    // Metode pentru fluxul jocului
    public void resetGame() {
        currentPhase = GamePhase.WAITING;
        currentPlayerIndex = 0;
        pot = 0;
        readyPlayers.clear();

        // Reseteaza toti jucatorii pentru joc nou
        for (Player player : players) {
            player.resetForNewRound();
        }
        dealer.resetForNewRound();
    }

    public void startGame() {
        if (players.isEmpty()) return;

        currentPhase = GamePhase.BETTING;
        currentPlayerIndex = 0;
        pot = 0;

        // Reseteaza toti jucatorii pentru runda noua
        for (Player player : players) {
            player.resetForNewRound();
        }
        dealer.resetForNewRound();
    }

    public boolean placeBet(int userId, double amount) {
        if (currentPhase != GamePhase.BETTING) {
            return false;
        }

        Player player = findPlayerByUserId(userId);
        if (player == null || player.hasBet()) {
            return false;
        }

        if (amount < minBet) {
            return false;
        }

        if (player.placeBet(amount)) {
            pot += amount;

            // Verifica daca toti jucatorii au pariat
            boolean allBet = true;
            for (Player p : players) {
                if (!p.hasBet()) {
                    allBet = false;
                    break;
                }
            }

            if (allBet) {
                dealInitialCards();
            }

            return true;
        }

        return false;
    }

    private void dealInitialCards() {
        currentPhase = GamePhase.DEALING;

        // Distribuie 2 carti fiecarui jucator
        for (Player player : players) {
            dealer.dealCardToPlayer(player);
            dealer.dealCardToPlayer(player);

            // Verifica pentru blackjack imediat
            if (player.hasBlackjack()) {
                player.setStatus(Player.PlayerStatus.BLACKJACK);
            } else {
                player.setStatus(Player.PlayerStatus.PLAYING);
            }
        }

        // Distribuie 2 carti dealerului (una cu fata in jos)
        dealer.dealCardToSelf(true);  // Cu fata in sus
        dealer.dealCardToSelf(false); // Cu fata in jos (ascunsa)

        // Trece la turile jucatorilor
        currentPhase = GamePhase.PLAYER_TURNS;
        currentPlayerIndex = 0;

        // Sare peste jucatorii cu blackjack
        skipToNextActivePlayer();
    }

    public boolean playerHit(int userId) {
        if (currentPhase != GamePhase.PLAYER_TURNS) {
            return false;
        }

        Player player = findPlayerByUserId(userId);
        if (player == null || player != getCurrentPlayer()) {
            return false;
        }

        // Distribuie o carte jucatorului
        dealer.dealCardToPlayer(player);

        // Verifica daca jucatorul a depasit 21 sau a obtinut 21
        if (player.isBusted() || player.getHandValue() == 21) {
            moveToNextPlayer();
        }

        return true;
    }

    public boolean playerStand(int userId) {
        if (currentPhase != GamePhase.PLAYER_TURNS) {
            return false;
        }

        Player player = findPlayerByUserId(userId);
        if (player == null || player != getCurrentPlayer()) {
            return false;
        }

        player.stand();
        moveToNextPlayer();

        return true;
    }

    public boolean playerSplit(int userId) {
        if (currentPhase != GamePhase.PLAYER_TURNS) {
            return false;
        }

        Player player = findPlayerByUserId(userId);
        if (player == null || player != getCurrentPlayer()) {
            return false;
        }

        if (!player.split()) {
            return false;
        }

        // Distribuie o carte fiecarei maini
        dealer.dealCardToPlayer(player);
        if (player.getSplitHand() != null) {
            Card card = dealer.getDeck().dealCard();
            player.getSplitHand().addCard(card);
        }

        return true;
    }

    public boolean playerDoubleDown(int userId) {
        if (currentPhase != GamePhase.PLAYER_TURNS) {
            return false;
        }

        Player player = findPlayerByUserId(userId);
        if (player == null || player != getCurrentPlayer()) {
            return false;
        }

        if (!player.doubleDown()) {
            return false;
        }

        // Distribuie o carte si stand automat
        dealer.dealCardToPlayer(player);

        return true;
    }

    public boolean playerBuyInsurance(int userId) {
        if (currentPhase != GamePhase.PLAYER_TURNS) {
            return false;
        }

        Player player = findPlayerByUserId(userId);
        if (player == null) {
            return false;
        }

        // Poate cumpara asigurare doar daca dealerul arata As
        if (!dealer.isShowingAce()) {
            return false;
        }

        return player.buyInsurance();
    }

    private void skipToNextActivePlayer() {
        // Sare peste jucatorii care au blackjack sau au depasit 21
        while (currentPlayerIndex < players.size()) {
            Player current = getCurrentPlayer();
            if (current != null &&
                current.getStatus() != Player.PlayerStatus.BLACKJACK &&
                current.getStatus() != Player.PlayerStatus.BUSTED) {
                break;
            }
            currentPlayerIndex++;
        }

        // Daca am trecut prin toti jucatorii, trece la tura dealerului
        if (currentPlayerIndex >= players.size()) {
            startDealerTurn();
        }
    }

    private void moveToNextPlayer() {
        currentPlayerIndex++;
        skipToNextActivePlayer();
    }

    private void startDealerTurn() {
        currentPhase = GamePhase.DEALER_TURN;

        // Dealerul joaca conform regulilor
        dealer.playTurn();

        // Calculeaza rezultatele
        calculateResults();
    }

    private void calculateResults() {
        currentPhase = GamePhase.RESULTS;

        int dealerValue = dealer.getHandValue();
        boolean dealerBusted = dealer.isBusted();
        boolean dealerBlackjack = dealer.hasBlackjack();

        // In Blackjack, casa (dealerul) plateste castigatorii direct - nu din pot
        // Pot-ul este doar pentru afisare (total pariuri pe masa)

        // Determina castigatorii si plateste
        for (Player player : players) {
            double bet = player.getCurrentBet();

            if (player.isBusted()) {
                // Jucatorul a depasit 21 - pierde pariul (deja dedus)
                player.loseBet();
                continue;
            }

            int playerValue = player.getHandValue();
            boolean playerBlackjack = player.hasBlackjack();

            if (dealerBusted) {
                // Dealerul a depasit 21, toti jucatorii care nu au depasit 21 castiga 1:1
                player.winBet(bet);
            } else if (playerBlackjack && !dealerBlackjack) {
                // Jucatorul are blackjack, dealerul nu - castiga 3:2 (1.5x pariu)
                player.winBet(bet * 1.5);
            } else if (playerValue > dealerValue) {
                // Jucatorul are valoare mai mare - castiga 1:1
                player.winBet(bet);
            } else if (playerValue == dealerValue) {
                // Egalitate - returneaza pariul (fara castiguri)
                player.pushBet();
            } else {
                // Jucatorul pierde - pariul deja dedus cand a fost plasat
                player.loseBet();
            }
        }

        // Reseteaza pot-ul la 0 dupa ce runda se termina
        pot = 0;

        currentPhase = GamePhase.FINISHED;
    }

    public void resetForNextRound() {
        readyPlayers.clear();
        startGame();
    }

    public void markPlayerReady(int userId) {
        readyPlayers.add(userId);
    }

    public boolean allPlayersReady() {
        if (players.isEmpty()) return false;
        // Verifica daca toti jucatorii din joc sunt gata
        for (Player player : players) {
            if (!readyPlayers.contains(player.getUserId())) {
                return false;
            }
        }
        return true;
    }

    public String getGameResultJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // Mana finala a dealerului
        sb.append("\"dealerHand\":");
        sb.append(dealer.getHand().toJson());
        sb.append(",\"dealerValue\":").append(dealer.getHandValue()).append(",");

        // Rezultatele jucatorilor
        sb.append("\"playerResults\":[");
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) sb.append(",");
            Player p = players.get(i);
            sb.append(String.format(java.util.Locale.US,
                "{\"userId\":%d,\"username\":\"%s\",\"handValue\":%d,\"result\":\"%s\",\"balance\":%.2f}",
                p.getUserId(), p.getUsername(), p.getHandValue(),
                p.getStatus().toString().toLowerCase(), p.getUser().getBalance()
            ));
        }
        sb.append("]}");

        return sb.toString();
    }

    // Serializare JSON pentru starea jocului
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"roomId\":\"").append(roomId).append("\",");
        sb.append("\"phase\":\"").append(currentPhase).append("\",");
        sb.append(String.format(java.util.Locale.US, "\"pot\":%.2f,", pot));
        sb.append("\"maxPlayers\":").append(maxPlayers).append(",");
        sb.append(String.format(java.util.Locale.US, "\"minBet\":%.2f,", minBet));

        // Jucatorul curent
        if (getCurrentPlayer() != null) {
            sb.append("\"currentTurn\":").append(getCurrentPlayer().getUserId()).append(",");
        }

        // Array-ul de jucatori
        sb.append("\"players\":[");
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(players.get(i).toJson());
        }
        sb.append("],");

        // Informatiile dealerului
        sb.append("\"dealer\":");
        boolean hideCards = currentPhase != GamePhase.RESULTS && currentPhase != GamePhase.FINISHED;
        sb.append(dealer.toJson(hideCards));

        sb.append("}");
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("GameRoom{id='%s', players=%d/%d, phase=%s, pot=%.2f}",
                roomId, players.size(), maxPlayers, currentPhase, pot);
    }
}
