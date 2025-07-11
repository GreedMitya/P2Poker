package GreedMitya.P2Poker.core;

import java.util.HashMap;
import java.util.Map;

public class Card implements Comparable<Card>{



    public static final Map<Suit, String> suitSymbols = new HashMap<>();
    public static final Map<Card.Rank, String> rankSymbols = new HashMap<>();
    private Rank rank;
    private Suit suit;


    public Card (Rank rank, Suit suit){
        this.rank = rank;
        this.suit = suit;
    }
    public Card(){

    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(rank, suit);
    }

    public Suit getSuit() {
        return suit;
    }
    public Rank getRank() {
        return rank;
    }

    static {
        suitSymbols.put(Card.Suit.Hearts, "H");
        suitSymbols.put(Card.Suit.Diamonds, "D");
        suitSymbols.put(Card.Suit.Clubs, "C");
        suitSymbols.put(Card.Suit.Spades, "S");

        rankSymbols.put(Card.Rank.Ace, "A");
        rankSymbols.put(Card.Rank.King, "K");
        rankSymbols.put(Card.Rank.Queen, "Q");
        rankSymbols.put(Card.Rank.Jack, "J");
        rankSymbols.put(Card.Rank.Ten, "10");
        rankSymbols.put(Card.Rank.Nine, "9");
        rankSymbols.put(Card.Rank.Eight, "8");
        rankSymbols.put(Card.Rank.Seven, "7");
        rankSymbols.put(Card.Rank.Six, "6");
        rankSymbols.put(Card.Rank.Five, "5");
        rankSymbols.put(Card.Rank.Four, "4");
        rankSymbols.put(Card.Rank.Three, "3");
        rankSymbols.put(Card.Rank.Two, "2");
    }

    @Override
    public String toString() {
        return rankSymbols.get(rank) + suitSymbols.get(suit);
    }

    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        Card card = (Card) object;
        return this.rank == card.rank && this.suit == card.suit;
    }

    @Override
    public int compareTo(Card other) {

        int rankComparison = Integer.compare(this.rank.ordinal(), other.rank.ordinal());
        if (rankComparison != 0) {
            return rankComparison;
        }


        return this.suit.ordinal() - other.suit.ordinal();
    }



    public enum Suit {
        Hearts,Spades,Diamonds,Clubs
    }
    public enum Rank {
        Two,Three,Four,Five,Six,Seven,Eight,Nine,Ten,Jack,Queen,King,Ace
    }

}
