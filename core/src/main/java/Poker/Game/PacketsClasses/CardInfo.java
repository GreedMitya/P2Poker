package Poker.Game.PacketsClasses;

import Poker.Game.core.Card;

import java.util.ArrayList;
import java.util.List;

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
