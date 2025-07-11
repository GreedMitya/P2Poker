package GreedMitya.P2Poker.core;



import java.util.List;

public class SidePot {
    private String name;
    private int sidePotCounter = 0;
    double amount;
    List<Player> eligiblePlayers;

    public SidePot(double amount, List<Player> eligiblePlayers) {
        sidePotCounter++;
        this.amount = amount;
        this.eligiblePlayers = eligiblePlayers;
        this.name = "SidePot_" + sidePotCounter;
    }
    public SidePot(){

    }
    public double getAmount(){
        return amount;
    }
    public List<Player> getPlayers(){
        return eligiblePlayers;
    }

    public void addPlayer(Player player) {
        eligiblePlayers.add(player);
    }

    public boolean containsPlayer(Player player) {
        return eligiblePlayers.contains(player);
    }

    public List<Player> getEligiblePlayers() {
        return eligiblePlayers;
    }
}
