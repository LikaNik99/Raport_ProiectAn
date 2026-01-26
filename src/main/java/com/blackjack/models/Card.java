package com.blackjack.models;

public class Card {
    public enum Suit {
        HEARTS("hearts"),
        DIAMONDS("diamonds"),
        CLUBS("clubs"),
        SPADES("spades");

        private final String value;

        Suit(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Rank {
        ACE("A", 11),
        TWO("2", 2),
        THREE("3", 3),
        FOUR("4", 4),
        FIVE("5", 5),
        SIX("6", 6),
        SEVEN("7", 7),
        EIGHT("8", 8),
        NINE("9", 9),
        TEN("10", 10),
        JACK("J", 10),
        QUEEN("Q", 10),
        KING("K", 10);

        private final String symbol;
        private final int value;

        Rank(String symbol, int value) {
            this.symbol = symbol;
            this.value = value;
        }

        public String getSymbol() {
            return symbol;
        }

        public int getValue() {
            return value;
        }
    }

    private final Suit suit;
    private final Rank rank;
    private boolean faceUp;

    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
        this.faceUp = true;
    }

    public Suit getSuit() {
        return suit;
    }

    public Rank getRank() {
        return rank;
    }

    public int getValue() {
        return rank.getValue();
    }

    public boolean isFaceUp() {
        return faceUp;
    }

    public void setFaceUp(boolean faceUp) {
        this.faceUp = faceUp;
    }

    public boolean isAce() {
        return rank == Rank.ACE;
    }

    @Override
    public String toString() {
        return rank.getSymbol() + " of " + suit.getValue();
    }

    public String toJson() {
        return String.format("{\"suit\":\"%s\",\"rank\":\"%s\",\"faceUp\":%b}",
                suit.getValue(), rank.getSymbol(), faceUp);
    }
}
