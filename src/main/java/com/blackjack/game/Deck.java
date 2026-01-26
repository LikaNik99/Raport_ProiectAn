package com.blackjack.game;

import com.blackjack.models.Card;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    private List<Card> cards;
    private int currentCardIndex;

    public Deck() {
        this.cards = new ArrayList<>();
        this.currentCardIndex = 0;
        initializeDeck();
    }

    /**
     * Initializeaza un pachet standard de 52 de carti
     */
    private void initializeDeck() {
        cards.clear();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
    }

    /**
     * Amesteca pachetul folosind Collections.shuffle
     */
    public void shuffle() {
        Collections.shuffle(cards);
        currentCardIndex = 0;
    }

    /**
     * Distribuie o carte din varful pachetului
     */
    public Card dealCard() {
        if (currentCardIndex >= cards.size()) {
            // Pachetul este gol, reamesteca
            reset();
        }
        return cards.get(currentCardIndex++);
    }

    /**
     * Verifica daca pachetul necesita reamestcare
     */
    public boolean needsShuffle() {
        return currentCardIndex >= cards.size() * 0.75; // Reamesteca la 75% utilizat
    }

    /**
     * Reseteaza si amesteca pachetul
     */
    public void reset() {
        initializeDeck();
        shuffle();
    }

    /**
     * Obtine numarul de carti ramase
     */
    public int getRemainingCards() {
        return cards.size() - currentCardIndex;
    }

    /**
     * Obtine numarul total de carti din pachet
     */
    public int getTotalCards() {
        return cards.size();
    }

    @Override
    public String toString() {
        return String.format("Deck{total=%d, remaining=%d}", getTotalCards(), getRemainingCards());
    }
}
