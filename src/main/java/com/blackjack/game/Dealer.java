package com.blackjack.game;

import com.blackjack.models.Card;

public class Dealer {
    private Hand hand;
    private Deck deck;

    public Dealer() {
        this.hand = new Hand();
        this.deck = new Deck();
        this.deck.shuffle();
    }

    public Hand getHand() {
        return hand;
    }

    public Deck getDeck() {
        return deck;
    }

    /**
     * Distribuie o carte unui jucator
     */
    public Card dealCardToPlayer(Player player) {
        Card card = deck.dealCard();
        player.addCard(card);
        return card;
    }

    /**
     * Distribuie o carte dealerului
     */
    public Card dealCardToSelf(boolean faceUp) {
        Card card = deck.dealCard();
        card.setFaceUp(faceUp);
        hand.addCard(card);
        return card;
    }

    /**
     * Arata toate cartile dealerului
     */
    public void revealHand() {
        hand.revealAll();
    }

    /**
     * Obtine valoarea mainii dealerului
     */
    public int getHandValue() {
        return hand.getValue();
    }

    /**
     * Obtine valoarea mainii vizibile (inainte de a arata cartea ascunsa)
     */
    public int getVisibleHandValue() {
        return hand.getVisibleValue();
    }

    /**
     * Verifica daca dealerul ar trebui sa traga conform regulilor standard
     * Dealerul trebuie sa traga la 16 sau mai putin, sa stea la 17 sau mai mult
     */
    public boolean shouldHit() {
        return hand.getValue() < 17;
    }

    /**
     * Dealerul joaca tura conform regulilor standard Blackjack
     * Returneaza true daca dealerul depaseste 21
     */
    public boolean playTurn() {
        // Arata intai cartea ascunsa
        revealHand();

        // Trage pana atinge 17 sau mai mult
        while (shouldHit()) {
            Card card = deck.dealCard();
            hand.addCard(card);
        }

        return hand.isBust();
    }

    /**
     * Verifica daca dealerul are blackjack
     */
    public boolean hasBlackjack() {
        return hand.isBlackjack();
    }

    /**
     * Verifica daca dealerul a depasit 21
     */
    public boolean isBusted() {
        return hand.isBust();
    }

    /**
     * Reseteaza dealerul pentru o noua runda
     */
    public void resetForNewRound() {
        hand.clear();

        // Reamesteca pachetul daca se termina
        if (deck.needsShuffle()) {
            deck.reset();
        }
    }

    /**
     * Obtine cartea vizibila (prima carte cu fata in sus)
     */
    public Card getVisibleCard() {
        for (Card card : hand.getCards()) {
            if (card.isFaceUp()) {
                return card;
            }
        }
        return null;
    }

    /**
     * Verifica daca dealerul arata un As (pentru asigurare)
     */
    public boolean isShowingAce() {
        Card visibleCard = getVisibleCard();
        return visibleCard != null && visibleCard.isAce();
    }

    @Override
    public String toString() {
        return String.format("Dealer{hand=%s}", hand);
    }

    public String toJson(boolean hideHiddenCards) {
        if (hideHiddenCards) {
            // Trimite TOATE cartile dar cu statusul lor faceUp
            // Clientul va randa cartile cu fata in jos cu sprite-ul de spate al cartii
            StringBuilder sb = new StringBuilder();
            sb.append("{\"visibleValue\":").append(getVisibleHandValue());
            sb.append(",\"handValue\":").append(getHandValue());
            sb.append(",\"cards\":");
            sb.append(hand.toJson()); // Send all cards with faceUp property
            sb.append("}");
            return sb.toString();
        } else {
            return String.format("{\"handValue\":%d,\"cards\":%s}",
                    getHandValue(), hand.toJson());
        }
    }
}
