package GreedMitya.P2Poker.core;

import GreedMitya.P2Poker.PacketsClasses.*;
import GreedMitya.P2Poker.Server.PokerServer;
import com.esotericsoftware.kryonet.Server;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class BettingManager {
    private final Lock betLock = new ReentrantLock();
    private PokerServer pokerServer;
    private Server server;
    private Map<Player, Double> playerBets;
    private PotManager potManager;
    private PlayerManager playerManager;
    private double pot;
    private double currentBet;
    private double smallBlind = 10;
    private double bigBlind = 20;
    private int dealerIndex;
    private int smallBlindIndex;
    private int bigBlindIndex;
    private int currentPlayerIndex;
    private int i = 0;
    boolean bettingRoundActive = true;
    public BettingManager(PlayerManager playerManager, PotManager potManager) {
        this.playerBets = new HashMap<>();
        this.potManager = potManager;
        this.playerManager = playerManager;
        this.pot = 0;
    }

    public BettingManager() {

    }

    public void reset() {
        pot = 0;
        currentBet = 0;
    }

    public double getPot() {
        return pot;
    }

    private void placeBet(Player player, double amount) {
        synchronized (betLock) {
            if (player.getBalance() > amount) {
                player.decreaseBalance(amount);
                player.setCurrentBetFromPlayers(player.getCurrentBetFromPlayer() + amount);
                pot += amount;
            } else {
                double allInAmount = player.getBalance();
                player.decreaseBalance(allInAmount);
                player.setAllIn();
                player.setCurrentBetFromPlayers(player.getCurrentBetFromPlayer() + allInAmount);
                pot += allInAmount;
            }
            PlayerBalanceUpdate upd = new PlayerBalanceUpdate();
            upd.name = player.getName();
            upd.newBalance = player.getBalance();
            server.sendToAllTCP(upd);

        }
    }

    public void setPokerServer(PokerServer pokerServer) {
        this.pokerServer = pokerServer;
        this.server = pokerServer.getServer();
        potManager.setServer(server);
    }

    public void setBlinds() {
        List<Player> activePlayers = playerManager.getActivePlayers();
        int size = activePlayers.size();
        if (size < 2) {
            throw new IllegalStateException("Not enough players to start the game.");
        }

        dealerIndex = (dealerIndex + 1) % size;
        smallBlindIndex = (dealerIndex + 1) % size;
        bigBlindIndex = (dealerIndex + 2) % size;

        Player dealerPlayer = activePlayers.get(dealerIndex);
        Player smallBlindPlayer = activePlayers.get(smallBlindIndex);
        Player bigBlindPlayer = activePlayers.get(bigBlindIndex);


        this.currentBet = bigBlind;
        pot += smallBlind + bigBlind;
        PotUpdate potUpdate = new PotUpdate(pot);
        server.sendToAllTCP(potUpdate);

        Logger.Game(smallBlindPlayer.getName() + " places small blind: " + smallBlind);
        Logger.Game(bigBlindPlayer.getName() + " places big blind: " + bigBlind);

        List<String> logicalOrder = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int idx = (dealerIndex + i) % size;
            logicalOrder.add(activePlayers.get(idx).getName());
        }
        PlayerOrderPacket orderPacket = new PlayerOrderPacket(logicalOrder);
        server.sendToAllTCP(orderPacket);

        smallBlindPlayer.decreaseBalance(smallBlind);
        smallBlindPlayer.setCurrentBetFromPlayers(smallBlind);
        playerBets.put(smallBlindPlayer, smallBlind);


        bigBlindPlayer.decreaseBalance(bigBlind);
        bigBlindPlayer.setCurrentBetFromPlayers(bigBlind);
        playerBets.put(bigBlindPlayer, bigBlind);
        List<String> messages = new ArrayList<>();

        messages.add(smallBlindPlayer.getName() + " places small blind: " + smallBlind);
        messages.add(bigBlindPlayer.getName() + " places big blind: " + bigBlind);
        BlindsNotification blindsNotification = new BlindsNotification(messages, dealerPlayer.getConnectionId());
        server.sendToAllTCP(blindsNotification);

        server.sendToAllTCP(new PlayerBalanceUpdate(smallBlindPlayer.getName(), smallBlindPlayer.getBalance()));
        server.sendToAllTCP(new PlayerBalanceUpdate(bigBlindPlayer.getName(), bigBlindPlayer.getBalance()));
    }

    public synchronized Action[] getAvailableActionsFor(Player player) {
        List<Action> list = new ArrayList<>();
        list.add(new Action("fold"));

        if (player.getCurrentBetFromPlayer() == currentBet) {
            list.add(new Action("check"));
        } else {
            double toCall = currentBet - player.getCurrentBetFromPlayer();
            boolean isAllIn = player.getBalance() <= toCall;

            double callAmount = isAllIn ? player.getBalance() : toCall;
            list.add(new Action("call", callAmount, 0, 0, isAllIn));
        }

        double minRaise = Math.max(currentBet * 2, currentBet + bigBlind);
        double maxRaise = player.getBalance();
        if (maxRaise > minRaise) {
            list.add(new Action("raise", 0, minRaise, maxRaise));
        }

        return list.toArray(new Action[0]);
    }


    public PlayerAction getPlayerActionWithTimeout(Player player, int seconds) {
        int connId = player.getConnectionId();
        Action[] avail = getAvailableActionsFor(player);

        try {
            return pokerServer
                .requestPlayerAction(connId, avail, seconds, player.getCurrentBetFromPlayer(), currentBet)
                .get(seconds + 1, TimeUnit.SECONDS);
        } catch (Exception e) {
            Logger.Game("Auto-decision for " + connId + ": " + e.getMessage());
            String fallback = (currentBet == player.getCurrentBetFromPlayer()) ? "check" : "fold";
            return new PlayerAction(fallback, 0);
        }
    }





    public synchronized void startBettingRound(BettingPhase phase) {
        Logger.Game("Betting round: " + phase);
        boolean roundEnded = false;
        int raiserIndex = -1;
        boolean restartRound = false;

        List<Player> activePlayers = playerManager.getActivePlayers();
        int playersCount = activePlayers.size();

        Set<Player> hasActed = new HashSet<>();

        while (!roundEnded) {
            activePlayers = playerManager.getActivePlayers();
            long nonAllIn = activePlayers.stream()
                .filter(p -> !p.isFolded() && !p.isAllIn())
                .count();
            if (nonAllIn==1) {
                Player caller = activePlayers.stream()
                    .filter(p -> !p.isAllIn())
                    .findFirst()
                    .get();
                System.out.println(caller.getCurrentBetFromPlayer()+ "");
                if ((caller.getCurrentBetFromPlayer() == currentBet)) {
                    roundEnded = true;
                    break;
                }
            }
            playersCount = activePlayers.size();

            for (int i = 0; i < playersCount; i++) {
                currentPlayerIndex = (phase == BettingPhase.PRE_FLOP)
                    ? (dealerIndex + 3 + i) % playersCount
                    : (dealerIndex + 1 + i) % playersCount;

                Player player = activePlayers.get(currentPlayerIndex);

                if (player.isFolded() || player.isAllIn()) continue;

                PlayerAction actionObj = getPlayerActionWithTimeout(player, 30);
                String action = actionObj.actionType.toLowerCase();
                double raiseAmount = actionObj.amount;

                hasActed.add(player);

                switch (action) {
                    case "fold":
                        player.fold();
                        pokerServer.sendChatMessage(player.getName() + " folds.");
                        break;


                    case "check":
                        if (currentBet == player.getCurrentBetFromPlayer()) {
                            pokerServer.sendChatMessage(player.getName() + " checks.");
                            server.sendToAllTCP(new CheckPacket());
                        } else {
                            pokerServer.sendChatMessage(player.getName() + " cannot check, folds instead.");
                            player.fold();
                        }
                        break;

                    case "call":
                        if (currentBet == 0 || currentBet == player.getCurrentBetFromPlayer()) {
                            pokerServer.sendChatMessage(player.getName() + " checks.");
                            playerBets.put(player, currentBet);
                            break;
                        }
                        if (player.getBalance() <= currentBet) {
                            playerBets.put(player, player.getBalance() + player.getCurrentBetFromPlayer());
                            placeBet(player, currentBet - player.getCurrentBetFromPlayer());
                            pokerServer.sendChatMessage(player.getName() + " All-in.");
                            break;
                        }
                        playerBets.put(player, currentBet);
                        pokerServer.sendChatMessage(player.getName() + " calls.");
                        placeBet(player, currentBet - player.getCurrentBetFromPlayer());
                        player.setCurrentBetFromPlayers(currentBet);
                        break;

                    case "raise":
                        double totalBet = raiseAmount - player.getCurrentBetFromPlayer();
                        if (player.getBalance() <= raiseAmount) {
                            double allInTotal = player.getBalance() + player.getCurrentBetFromPlayer();
                            pokerServer.sendChatMessage(player.getName() + " goes all-in with " + allInTotal);
                            playerBets.put(player, allInTotal);
                            currentBet = allInTotal;
                            placeBet(player, allInTotal);
                            raiserIndex = currentPlayerIndex;
                            hasActed.clear();
                            hasActed.add(player);
                            restartRound = true;
                            break;
                        }
                        if (player.getBalance() > totalBet) {
                            pokerServer.sendChatMessage(player.getName() + " raises to " + raiseAmount);
                            currentBet = raiseAmount;
                            playerBets.put(player, currentBet);
                            placeBet(player, totalBet);
                            raiserIndex = currentPlayerIndex;
                            hasActed.clear();
                            hasActed.add(player);
                            restartRound = true;
                        }
                        break;
                }
                nonAllIn = activePlayers.stream()
                    .filter(p -> !p.isFolded() && !p.isAllIn())
                    .count();

                boolean allMatched = activePlayers.stream()
                    .filter(p -> !p.isFolded() && !p.isAllIn())
                    .allMatch(p -> p.getCurrentBetFromPlayer() == currentBet);

                boolean allActed = activePlayers.stream()
                    .filter(p -> !p.isFolded() && !p.isAllIn())
                    .allMatch(hasActed::contains);

                if ((allMatched && allActed) || (nonAllIn == 1 && allMatched)) {
                    roundEnded = true;
                    break;
                }
            }

            if (restartRound) {
                restartRound = false;
                continue;
            }

            roundEnded = checkIfRoundEnded();
        }

        Logger.Game("Round " + phase + " finished!");
        Logger.Game("Total pot: " + pot);

        HashMap<Integer, Double> betsToSend = new HashMap<>();
        for (Map.Entry<Player, Double> entry : playerBets.entrySet()) {
            betsToSend.put(entry.getKey().getConnectionId(), entry.getValue());
        }

        BetUpdatePack betUpdatePack = new BetUpdatePack();
        betUpdatePack.setBets(betsToSend);
        server.sendToAllTCP(betUpdatePack);

        if (phase == BettingPhase.PRE_FLOP) potManager.setPreFlop(playerBets);
        if (phase == BettingPhase.FLOP) potManager.setFlop(playerBets);
        if (phase == BettingPhase.TURN) potManager.setTurn(playerBets);
        if (phase == BettingPhase.RIVER) potManager.setRiver(playerBets);

        betsToSend.clear();
        playerBets.clear();
        playerManager.getActivePlayers().forEach(p -> p.setCurrentBetFromPlayers(0));
        currentBet = 0;
        pot = 0;
    }



    private synchronized boolean checkIfRoundEnded() {
        if (playerManager.getActivePlayers().stream()
            .filter(p -> !p.isFolded()&&!p.isAllIn())
            .allMatch(p -> p.getCurrentBetFromPlayer() == currentBet)) return true;
        List<Player> list = new ArrayList<>();
        for (Player player : playerManager.getActivePlayers()) {
            if (!player.isAllIn()) {
                list.add(player);
            }
        }
        return (list.isEmpty()||list.size()==1);
    }

    public enum BettingPhase {
        PRE_FLOP, FLOP, TURN, RIVER;
    }
}
