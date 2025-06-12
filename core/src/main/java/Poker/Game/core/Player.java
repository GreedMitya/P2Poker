package Poker.Game.core;
import Poker.Game.PacketsClasses.FoldNotification;
import Poker.Game.PacketsClasses.PlayerBetUpdate;
import com.esotericsoftware.kryonet.Server;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class Player {
    private Server server;
    private String name;
    private boolean folded = false;
    private boolean isAllIn = false;
    private double balance;
    private ArrayList<Card> hand;
    private double currentBetFromPlayer;
    private List<Card> combination;
    private List<Card> totalHand;
    private int handValue;
    private int connectionId;

    public Player(String name) {
        this.name = name;
        this.balance = 1000;
        this.hand = new ArrayList<>();
        this.totalHand = new ArrayList<>();
        this.isAllIn = false;
        this.folded = false;
    }

    public Player(){

    }
    public void setServer(Server server) {
        this.server = server;
    }

    public void fold(){
        this.folded = true;
        FoldNotification foldNotification = new FoldNotification(getConnectionId());
        server.sendToAllTCP(foldNotification);
    }

    public boolean isFolded(){
        return folded;
    }

    public void reset(){
        this.folded = false;
        this.isAllIn = false;
    }

    public void setCombination(){
        combination = HandEvaluator.getCombination(totalHand);
    }

    public List<Card> getCombination(){
        return combination;
    }

    public void evaluateHand(){
        handValue = HandEvaluator.evaluateHand(totalHand);
    }

    public int getHandValue(){
        return handValue;
    }

    public List<Card> getTotalHand(){
        return totalHand;
    }

    public void drawCards(Deck deck) {
        hand.add(deck.dealCard());
        hand.add(deck.dealCard());
    }

    public void clearHand() {
        hand.clear();
    }

    public ArrayList<Card> getHand() {
        return hand;
    }

    public String getName() {
        return name;
    }

    public double getBalance() {
        return balance;
    }

    public void setTotalHand(){
        totalHand = HandCollector.getTotalHand();
    }

    public void setCurrentBetFromPlayers(double currentBetFromPlayer){
        this.currentBetFromPlayer = currentBetFromPlayer;
        PlayerBetUpdate playerBetUpdate = new PlayerBetUpdate(getConnectionId(),currentBetFromPlayer);
        server.sendToAllTCP(playerBetUpdate);
    }

    public void decreaseBalance(double amount) {
        if (amount <= 0) return; // Нельзя вычитать отрицательные значения

        if (balance >= amount) {
            balance -= amount;
        } else {
            balance = 0;
            isAllIn = true;
        }
    }

    public double getCurrentBetFromPlayer(){
        return currentBetFromPlayer;
    }

    public void increaseBalance(double amount) {
        if (amount > 0) {
            balance += amount; // Предотвращаем отрицательные пополнения
        }
    }

    public boolean isAllIn() {
        return isAllIn;
    }

    public void setAllIn() {
        isAllIn = true;
    }

    public String getAction() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Choose an action for " + getName() + ": (check, call, fold, raise)");
        String action = scanner.nextLine().trim().toLowerCase();

        switch (action) {
            case "check":
                return "check";
            case "call":
                return "call";
            case "fold":
                fold();
                return "fold";
            case "raise":
                return "raise";
            default:
                System.out.println("Invalid option. Please choose again.");
                return getAction();  // рекурсивный вызов для повторного ввода
        }
    }

    public double decideRaiseAmount() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Choose the amount to raise (your balance: " + balance + "): ");
        double raiseAmount = scanner.nextDouble();
        if (raiseAmount <= balance) {
            return raiseAmount;
        } else {
            System.out.println("You cannot raise more than your balance. Raising to your full balance: " + balance);
            return balance;
        }
    }

    public String getNameofCombination(){
        if (getHandValue() >= 10_000_000_0) return "Royal flush!";
        if (getHandValue() >= 9_000_000_0) return "Straight flush!";
        if (getHandValue() >= 8_000_000_0) return "Four of a kind!";
        if (getHandValue() >= 7_000_000_0) return "Full house!";
        if (getHandValue() >= 6_000_000_0) return "Flush!";
        if (getHandValue() >= 5_000_000_0) return "Straight!";
        if (getHandValue() >= 4_000_000_0) return "Three of a kind!";
        if (getHandValue() >= 3_000_000_0) return "Two pairs!";
        if (getHandValue() >= 2_000_000_0) return "One pair!";
        else return "High cards!";
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    public int getConnectionId() {
        return connectionId;
    }
}
