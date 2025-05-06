package Poker.Game.PacketsClasses;

public class PlayerBalanceUpdate {
    public String name;
    public double newBalance;


    public PlayerBalanceUpdate(){
    }

    public PlayerBalanceUpdate(String name, double balance) {
        this.name = name;
        this.newBalance = balance;
    }
}
