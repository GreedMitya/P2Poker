package GreedMitya.P2Poker.core;

import java.util.ArrayList;
import java.util.Collections;

public class Deck {
    public ArrayList<Card> cards;

    public Deck() {
        cards = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                cards.add(new Card(rank, suit));
            }
        }
    }


    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Card dealCard() {
        if (cards.isEmpty()) return null;
        return cards.remove((0));
    }

    public void returnCard(Card card) {
        cards.add(card);
    }
    public int getSize(){
        return cards.size();
    }
}

