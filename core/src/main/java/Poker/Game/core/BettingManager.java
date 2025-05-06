package Poker.Game.core;

import Poker.Game.PacketsClasses.*;
import Poker.Game.Server.PokerServer;
import com.esotericsoftware.kryonet.Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;


public class BettingManager {
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

    private synchronized void placeBet(Player player, double amount) {
        if (player.getBalance() > amount) {
            // Если хватает денег, списываем полную сумму
            player.decreaseBalance(amount);
            player.setCurrentBetFromPlayers(player.getCurrentBetFromPlayer() + amount);
            pot += amount;
        } else {
            // Игрок идет All-in
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

        Player smallBlindPlayer = activePlayers.get(smallBlindIndex);
        Player bigBlindPlayer = activePlayers.get(bigBlindIndex);

        smallBlindPlayer.decreaseBalance(smallBlind);
        smallBlindPlayer.setCurrentBetFromPlayers(smallBlind);
        playerBets.put(smallBlindPlayer, smallBlind);
        //Нужны ли эти две строки вообще?

        bigBlindPlayer.decreaseBalance(bigBlind);
        bigBlindPlayer.setCurrentBetFromPlayers(bigBlind);
        playerBets.put(bigBlindPlayer, bigBlind);

        this.currentBet = bigBlind;
        pot += smallBlind + bigBlind;
        PotUpdate potUpdate = new PotUpdate(pot);
        server.sendToAllTCP(potUpdate);

        Logger.Game(smallBlindPlayer.getName() + " places small blind: " + smallBlind);
        Logger.Game(bigBlindPlayer.getName() + " places big blind: " + bigBlind);

        BlindsNotification note = new BlindsNotification("Blinds: \n" + smallBlindPlayer.getName().toString() + " places small blind: " + smallBlind + "\n" + bigBlindPlayer.getName().toString() + " places big blind: " + bigBlind);
        server.sendToAllTCP(note);
        server.sendToAllTCP(new PlayerBalanceUpdate(smallBlindPlayer.getName(), smallBlindPlayer.getBalance()));
        server.sendToAllTCP(new PlayerBalanceUpdate(bigBlindPlayer.getName(), bigBlindPlayer.getBalance()));

    }


    /*public synchronized String getPlayerActionWithTimeout(Player player, int seconds) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(player::getAction);

        try {
            return future.get(seconds, TimeUnit.SECONDS); // Ждём действие игрока
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            Logger.Game("Time out! ");
            return (currentBet == 0 || player.getCurrentBetFromPlayer() == currentBet) ? "check" : "fold"; // Авто-действие
        } finally {
            executor.shutdown();
        }
    }
     */
    public Action[] getAvailableActionsFor(Player player) {
        List<Action> list = new ArrayList<>();
        // всегда доступно «фолд»
        list.add(new Action("fold"));
        // если можем чекнуть
        if (player.getCurrentBetFromPlayer() == currentBet) {
            list.add(new Action("check"));
        } else {
            double toCall = currentBet - player.getCurrentBetFromPlayer();
            list.add(new Action("call", toCall, 0, 0));
        }
        // если можем рейзить
        double minRaise = Math.max(currentBet * 2, currentBet + bigBlind);
        double maxRaise = player.getBalance();
        if (maxRaise > minRaise) {
            list.add(new Action("raise", 0, minRaise, maxRaise));
        }
        // в конце переконвертировать в массив
        Action[] avail = list.toArray(new Action[0]);
        return avail;
    }


    public PlayerAction getPlayerActionWithTimeout(Player player, int seconds) {
        int connId = player.getConnectionId();
        Action[] avail = getAvailableActionsFor(player);

        try {
            return pokerServer
                .requestPlayerAction(connId, avail, seconds)
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
        int playersCount = playerManager.getActivePlayers().size();

        while (!roundEnded) {

            List<Player> list = new ArrayList<>();
            for (Player player1 : playerManager.getActivePlayers()) {
                if (!player1.isAllIn()) {
                    list.add(player1);
                }
            }
            if (list.size() == 1 && currentBet == 0) {
                break; // Если остался один игрок, завершаем круг
            }

            boolean allPlayersMatchedBet = playerManager.getActivePlayers().stream()
                .filter(p -> !p.isFolded() && !p.isAllIn())
                .allMatch(p -> p.getCurrentBetFromPlayer() == currentBet);


            for (int i = 0; i < playersCount; i++) {
                currentPlayerIndex = (dealerIndex + 3 + i) % playersCount;
                if (phase != BettingPhase.PRE_FLOP) {
                    currentPlayerIndex = (dealerIndex + 1 + i) % playersCount;
                }

                Player player = playerManager.getActivePlayers().get(currentPlayerIndex);


                if (player.isFolded() && player.isAllIn()) continue;
                List<Player> result = new ArrayList<>();
                for (Player p : playerManager.getActivePlayers()) {
                    if (!player.isAllIn()) {
                        result.add(p);
                    }
                }
                if (result.isEmpty()) {
                    roundEnded = true;
                    break;
                }





                // Проверяем, нужно ли завершить круг после рейза
                if (allPlayersMatchedBet && raiserIndex != -1 && currentPlayerIndex == raiserIndex) {
                    roundEnded = true;
                    break;
                }





                if (player.isAllIn()) continue;


                PlayerAction actionObj = getPlayerActionWithTimeout(player, 30);
                String action = actionObj.actionType.toLowerCase();
                double raiseAmount = actionObj.amount;

                switch (action.toLowerCase()) {
                    case "fold":
                        player.fold();
                        System.out.println(player.getName() + " folds.");
                        pokerServer.sendChatMessage(player.getName() + " folds.");
                        break;

                    case "check":
                        if (currentBet == player.getCurrentBetFromPlayer()) {
                            System.out.println(player.getName() + " checks.");
                            pokerServer.sendChatMessage(player.getName() + " checks.");
                        } else {
                            System.out.println("Cannot check, must call/raise/fold!");
                            player.fold();
                        }
                        break;

                    case "call":
                        if (currentBet == 0 || currentBet == player.getCurrentBetFromPlayer()) {
                            System.out.println(player.getName() + " checks.");
                            playerBets.put(player, currentBet);
                            break;
                        }
                        if (player.getBalance() <= currentBet) {
                            System.out.println(player.getName() + " All-in.");
                            pokerServer.sendChatMessage(player.getName() + " All-in.");
                            playerBets.put(player, player.getBalance() + player.getCurrentBetFromPlayer());
                            placeBet(player, currentBet - player.getCurrentBetFromPlayer());
                            player.setCurrentBetFromPlayers(currentBet);
                            break;
                        }
                        if (player.getBalance() > currentBet) {
                            playerBets.put(player, currentBet);
                            System.out.println(player.getName() + " calls.");
                            pokerServer.sendChatMessage(player.getName() + " calls.");
                            placeBet(player, currentBet - player.getCurrentBetFromPlayer());
                            player.setCurrentBetFromPlayers(currentBet);
                        }
                        break;

                    case "raise":
                        double totalBet = raiseAmount - player.getCurrentBetFromPlayer();              // Сумма рейза
                        if (player.getBalance() <= raiseAmount) {
                            System.out.println(player.getName() + " goes all-in with " + (player.getBalance() + player.getCurrentBetFromPlayer()));
                            pokerServer.sendChatMessage(player.getName() + " goes all-in with " + (player.getBalance() + player.getCurrentBetFromPlayer()));
                            playerBets.put(player, player.getBalance() + player.getCurrentBetFromPlayer());
                            currentBet = player.getBalance() + player.getCurrentBetFromPlayer();
                            placeBet(player, player.getBalance() + player.getCurrentBetFromPlayer());
                            raiserIndex = currentPlayerIndex; // Запоминаем последнего рейзера
                            restartRound = true;
                            break;
                        }
                        if (player.getBalance() > totalBet) {
                            System.out.println(player.getName() + " raises to " + raiseAmount);
                            pokerServer.sendChatMessage(player.getName() + " raises to " + raiseAmount);
                            currentBet = raiseAmount;
                            playerBets.put(player, currentBet);
                            placeBet(player, totalBet);
                            raiserIndex = currentPlayerIndex; // Запоминаем последнего рейзера
                            restartRound = true;
                        }
                        break;

                    default:
                        System.out.println("Invalid action. Please choose fold, check, call, or raise.");
                }


            }
            if (restartRound) {
                restartRound = false;
                continue;
            }

            roundEnded = checkIfRoundEnded();
        }
        System.out.println("Round " + phase + " finished!");
        pokerServer.sendChatMessage("Round " + phase + " finished!");
        System.out.println("Total pot: " + pot);
        //potManager.setMainPot(pot);//Перемещаем пот с улицы в ПотМенеджер;
        // Дальше нужно реализовать перенос учавствовавших игроков и их ставки в ПотМенеджер;
        //System.out.println(potManager.getMainPot());
        // Записывает кто сколько положил в конце раунда! в Map;
        // Дальше этот список тоже передаем в PotManager для обработки и разбивки на сайд поты!
        //System.out.println("Transfer to potManger: " + playerBets.toString());
        if(phase == BettingPhase.PRE_FLOP) potManager.setPreFlop(playerBets);
        if(phase == BettingPhase.FLOP) potManager.setFlop(playerBets);
        if(phase == BettingPhase.TURN) potManager.setTurn(playerBets);
        if(phase == BettingPhase.RIVER) potManager.setRiver(playerBets);
        // А затем обнуляем перед следующим раундом!
        playerBets.clear();
        playerManager.getActivePlayers().forEach(player -> player.setCurrentBetFromPlayers(0));
        currentBet = 0;
        pot = 0;

    }


    private boolean checkIfRoundEnded() {
        if (playerManager.getActivePlayers().stream()
            .filter(p -> !p.isFolded()) //&& !p.isAllIn()
            .allMatch(p -> p.getCurrentBetFromPlayer() == currentBet)) return true;//&& !p.isAllIn()
        List<Player> list = new ArrayList<>();
        for (Player player : playerManager.getActivePlayers()) {
            if (!player.isAllIn()) {
                list.add(player);
            }
        }
        return (list.isEmpty());
    }

    public enum BettingPhase {
        PRE_FLOP, FLOP, TURN, RIVER;
    }
}
