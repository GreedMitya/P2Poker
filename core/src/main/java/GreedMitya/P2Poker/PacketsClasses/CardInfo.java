package GreedMitya.P2Poker.PacketsClasses;

import GreedMitya.P2Poker.core.Card;

import java.util.ArrayList;

public class CardInfo {
    private ArrayList<Card> Hand;

    public CardInfo(ArrayList<Card> Hand) {
        this.Hand = Hand;
    }
    public CardInfo(){

    }

    public ArrayList<Card> getHand() {
        return Hand;
    }
}
