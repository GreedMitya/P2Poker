package Poker.Game.PacketsClasses;

public class PlayerAction {
    public String actionType; // "fold", "call", "raise", ...
    public double amount;

    public PlayerAction(){}

    public PlayerAction(String actionType, double amount) {
        this.actionType = actionType;
        this.amount = amount;
    }
}

