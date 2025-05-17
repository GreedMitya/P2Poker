package Poker.Game.PacketsClasses;

import java.util.HashMap;

public class BetUpdatePack {
    private HashMap<Integer, Double> Bets;

    public BetUpdatePack(){}

    public void setBets(HashMap<Integer, Double> Bets){
        this.Bets = Bets;
    }
    public HashMap<Integer, Double> getBets(){
        return Bets;
    }
}
