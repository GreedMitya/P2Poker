package Poker.Game.PacketsClasses;

import Poker.Game.core.Card;

import java.util.ArrayList;
import java.util.List;

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
