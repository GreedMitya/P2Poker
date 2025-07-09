package GreedMitya.P2Poker.PacketsClasses;

public class FoldNotification {
    public int playerId;


    public FoldNotification(){
    }
    public FoldNotification(int playerId){
        this.playerId = playerId;
    }
}
