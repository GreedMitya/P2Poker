package GreedMitya.P2Poker.core;


import GreedMitya.P2Poker.PacketsClasses.PotUpdate;
import com.esotericsoftware.kryonet.Server;

import java.util.*;

public class PotManager {
    private Server server;
    private Map<Player, Double> PreFlop = new HashMap<>();
    private Map<Player, Double> Flop = new HashMap<>();
    private Map<Player, Double> Turn = new HashMap<>();
    private Map<Player, Double> River = new HashMap<>();
    private List<SidePot> sidePots = new ArrayList<>();
    private double mainPot = 0.0;
    double finalPot;
    public PotManager(){

    }

    public void setServer(Server server) {
        this.server = server;
    }

    public void setMainPot(double pot) {
        this.mainPot += pot;
    }

    public double getMainPot() {
        return mainPot;
    }

    public List<SidePot> getSidePots() {
        return sidePots;
    }

    public void reset() {
        this.mainPot = 0;
        this.finalPot = 0;
        this.sidePots.clear();
        this.PreFlop.clear();
        this.Flop.clear();
        this.Turn.clear();
        this.River.clear();
    }

    public void finalizeSidePots() {
        System.out.println("Finalizing all pots...");
        processStreetBets(PreFlop, "PreFlop");
        processStreetBets(Flop, "Flop");
        processStreetBets(Turn, "Turn");
        processStreetBets(River, "River");
        System.out.println("Final Main Pot: " + mainPot);
    }


    private void processStreetBets(Map<Player, Double> streetBets, String streetName) {

        if (streetBets.isEmpty()) {
            //System.out.println(streetName + " bets are empty, skipping...");
            return;
        }
        List<Map.Entry<Player, Double>> sortedBets = new ArrayList<>(streetBets.entrySet());
        sortedBets.sort(Comparator.comparingDouble(Map.Entry::getValue));

        double totalStreetAmount = streetBets.values().stream().mapToDouble(Double::doubleValue).sum();

        while (!sortedBets.isEmpty()) {
            double minBet = sortedBets.get(0).getValue();
            List<Player> eligiblePlayers = new ArrayList<>();
            double sidePotAmount = 0;

            for (Iterator<Map.Entry<Player, Double>> iterator = sortedBets.iterator(); iterator.hasNext(); ) {
                Map.Entry<Player, Double> entry = iterator.next();
                Player player = entry.getKey();
                double bet = entry.getValue();

                if (bet >= minBet) {
                    eligiblePlayers.add(player);
                    sidePotAmount += minBet;
                    entry.setValue(bet - minBet);
                }

                if (entry.getValue() == 0) {
                    iterator.remove();
                }
            }

            if (!eligiblePlayers.isEmpty()) {
                SidePot sidePot = new SidePot(sidePotAmount, eligiblePlayers);
                sidePots.add(sidePot);
                //System.out.println("Created Side Pot: " + sidePotAmount + " - Players: " + eligiblePlayers);
            }
        }
        mainPot += streetBets.values().stream().mapToDouble(Double::doubleValue).sum();
        finalPot += totalStreetAmount;
        PotUpdate potUpdate = new PotUpdate(finalPot);
        server.sendToAllTCP(potUpdate);
    }
    public double getTotalPot() {
        double total = mainPot;
        for (SidePot sidePot : sidePots) {
            total += sidePot.getAmount();
        }
        return total;

    }

    public void setPreFlop(Map<Player, Double> PreFlop) {
        this.PreFlop = new HashMap<>(PreFlop);
        processStreetBets(this.PreFlop, "PreFlop");
    }

    public void setFlop(Map<Player, Double> Flop) {
        this.Flop = new HashMap<>(Flop);
        processStreetBets(this.Flop, "Flop");
    }

    public void setTurn(Map<Player, Double> Turn) {
        this.Turn = new HashMap<>(Turn);
        processStreetBets(this.Turn, "Turn");
    }

    public void setRiver(Map<Player, Double> River) {
        this.River = new HashMap<>(River);
        processStreetBets(this.River, "River");
    }
}
