package com.blackjack.game;

import com.blackjack.models.Card;
import java.util.ArrayList;
import java.util.List;

public class Hand {
    private List<Card> cards;

    public Hand() {
        this.cards = new ArrayList<>();
    }

    public void addCard(Card card) {
        cards.add(card);
    }

    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    public int getCardCount() {
        return cards.size();
    }

    public void clear() {
        cards.clear();
    }

    /**
     * Calculeaza cea mai buna valoare pentru aceasta mana.
     * Asii pot fi 1 sau 11, asa ca calculam valoarea optima.
     */
    public int getValue() {
        int value = 0;
        int aceCount = 0;

        // Prima trecere: numara asii si adauga valorile celorlalte carti
        for (Card card : cards) {
            if (card.isAce()) {
                aceCount++;
                value += 11; // Numara asii ca 11 initial
            } else {
                value += card.getValue();
            }
        }

        // Ajusteaza pentru asi daca suntem peste 21
        while (value > 21 && aceCount > 0) {
            value -= 10; // Converteste un as din 11 in 1
            aceCount--;
        }

        return value;
    }

    public boolean isBust() {
        return getValue() > 21;
    }

    public boolean isBlackjack() {
        return cards.size() == 2 && getValue() == 21;
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /**
     * Verifica daca aceasta mana poate fi impartita (doua carti de acelasi rang)
     */
    public boolean canSplit() {
        if (cards.size() != 2) {
            return false;
        }
        // Verifica daca ambele carti au acelasi rang
        return cards.get(0).getRank().equals(cards.get(1).getRank());
    }

    /**
     * Imparte aceasta mana - elimina si returneaza a doua carte
     */
    public Card splitHand() {
        if (!canSplit()) {
            return null;
        }
        return cards.remove(1);
    }

    /**
     * Obtine cartile vizibile (pentru mana dealerului unde o carte ar putea fi ascunsa)
     */
    public List<Card> getVisibleCards() {
        List<Card> visible = new ArrayList<>();
        for (Card card : cards) {
            if (card.isFaceUp()) {
                visible.add(card);
            }
        }
        return visible;
    }

    /**
     * Obtine valoarea doar a cartilor vizibile
     */
    public int getVisibleValue() {
        int value = 0;
        int aceCount = 0;

        for (Card card : cards) {
            if (card.isFaceUp()) {
                if (card.isAce()) {
                    aceCount++;
                    value += 11;
                } else {
                    value += card.getValue();
                }
            }
        }

        while (value > 21 && aceCount > 0) {
            value -= 10;
            aceCount--;
        }

        return value;
    }

    public void revealAll() {
        for (Card card : cards) {
            card.setFaceUp(true);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(cards.get(i).toString());
        }
        sb.append("] Value: ").append(getValue());
        return sb.toString();
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(cards.get(i).toJson());
        }
        sb.append("]");
        return sb.toString();
    }
}
