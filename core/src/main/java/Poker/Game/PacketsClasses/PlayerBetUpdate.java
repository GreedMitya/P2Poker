package Poker.Game.PacketsClasses;

public class PlayerBetUpdate {
    public int playerId;
    public double amount;

    public PlayerBetUpdate(){

    }

    public PlayerBetUpdate(int connectionId, double currentBetFromPlayer) {
        this.playerId = connectionId;
        this.amount = currentBetFromPlayer;
    }
}

