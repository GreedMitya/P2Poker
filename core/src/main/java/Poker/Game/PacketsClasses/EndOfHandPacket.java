package Poker.Game.PacketsClasses;

import Poker.Game.core.Card;

import java.util.List;
import java.util.Map;

public class EndOfHandPacket {
    private Map<Integer, List<Card>> handsByPlayerId;
    private List<Integer> winnerIds;
    private List<List<Card>> winningCards;
    private double amountWon;
    private List<String> combinationNames;

    // Геттеры
    public Map<Integer, List<Card>> getHandsByPlayerId() { return handsByPlayerId; }
    public List<Integer> getWinnerIds() { return winnerIds; }
    public List<List<Card>> getWinningCards() { return winningCards; }
    public double getAmountWon() { return amountWon; }
    public List<String> getCombinationNames() { return combinationNames; }
    // Сеттеры
    public void setHandsByPlayerId(Map<Integer, List<Card>> handsByPlayerId) {
        this.handsByPlayerId = handsByPlayerId;
    }

    public void setWinnerIds(List<Integer> winnerIds) {
        this.winnerIds = winnerIds;
    }

    public void setWinningCards(List<List<Card>> winningCards) {
        this.winningCards = winningCards;
    }

    public void setAmountWon(double amountWon) {
        this.amountWon = amountWon;
    }

    public void setCombinationNames(List<String> combinationNames) {
        this.combinationNames = combinationNames;
    }

    public EndOfHandPacket(){
    }
    public EndOfHandPacket(Map<Integer, List<Card>> handsByPlayerId) {
        this.handsByPlayerId = handsByPlayerId;
    }

}
