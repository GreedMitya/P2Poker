package GreedMitya.P2Poker.PacketsClasses;

public class PlayerAction {
    public String actionType;
    public double amount;

    public PlayerAction(){}

    public PlayerAction(String actionType, double amount) {
        this.actionType = actionType;
        this.amount = amount;
    }
}

