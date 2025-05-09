package Poker.Game.PacketsClasses;

import Poker.Game.core.Card;
import java.util.List;

public class WinnerInfo {
    public List<Integer> playerIds;
    public List<List<Card>> winningCards;
    public List<String> combinationNames;
    public double amountWon;

    public WinnerInfo() {} // для Kryo

    public WinnerInfo(List<Integer> playerIds, List<List<Card>> winningCards, List<String> combinationNames, double amountWon) {
        this.playerIds = playerIds;
        this.winningCards = winningCards;
        this.combinationNames = combinationNames;
        this.amountWon = amountWon;
    }
}



