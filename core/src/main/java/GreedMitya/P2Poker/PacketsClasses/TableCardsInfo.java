package GreedMitya.P2Poker.PacketsClasses;

import GreedMitya.P2Poker.core.Card;

import java.util.ArrayList;

public class TableCardsInfo {
    private ArrayList<Card> tableCards;

    public TableCardsInfo(){
    }
    public TableCardsInfo(ArrayList<Card> tableCards) {
        this.tableCards = tableCards;
    }

    public ArrayList<Card> getCards() {
        return tableCards;
    }
}
