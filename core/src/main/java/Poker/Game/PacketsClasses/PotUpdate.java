package Poker.Game.PacketsClasses;

public class PotUpdate {
    private double potAmount;


    public double getPotAmount() {
        return potAmount;
    }
    public PotUpdate(double pot){
        this.potAmount =pot;
    }
    public PotUpdate(){

    }


    public void setPotAmount(double potAmount) {
        this.potAmount = potAmount;
    }
}
