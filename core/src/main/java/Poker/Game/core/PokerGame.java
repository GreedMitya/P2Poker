package Poker.Game.core;

import Poker.Game.PacketsClasses.GameStats;
import Poker.Game.PacketsClasses.Logger;
import Poker.Game.PacketsClasses.WinnerInfo;
import Poker.Game.Server.PokerServer;
import com.esotericsoftware.kryonet.Server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Main Game Manager Class
public class PokerGame {
    private PokerServer pokerServer;

    private Server server;
    public Deck deck;
    public Table table;
    public BettingManager bettingManager;
    public PlayerManager playerManager;
    private int roundCounter = 0;
    private List<Player> activePlayers;
    private BettingManager.BettingPhase bettingPhase;
    private PotManager potManager;
    public PokerGame(ArrayList<Player> players) {
        this.deck = new Deck();
        this.table = new Table(deck);
        this.potManager = new PotManager();
        this.playerManager = new PlayerManager(players);
        this.bettingManager = new BettingManager(playerManager, potManager);
    }

    public PokerGame(){

    }
    public void setPokerServer(PokerServer pokerServer) {
        this.pokerServer = pokerServer;
    }

    public void startGame() {
        while (playerManager.getPlayers().size() > 1) {
            roundCounter++;
            Logger.Game("\nStarting Round: " + roundCounter);
            deck.shuffle();
            Logger.Game("Active players: " + playerManager.getActivePlayers().size());
            GameStats stats = new GameStats(roundCounter,playerManager.getActivePlayers().size());
            server.sendToAllTCP(stats);
            playerManager.prepareForRound(deck);
            bettingManager.reset();
            playRound(); //- КЛЮЧ К СТАРТУ
        }
        System.out.println("Game Over. Winner: " + playerManager.getActivePlayers().get(0).getName());
        pokerServer.sendChatMessage("Game Over. Winner: " + playerManager.getActivePlayers().get(0).getName());
        System.exit(0);

    }
    public void setServer(Server server) {
        this.server = server;
    }

    public void resetBets() {
        for (Player player : playerManager.getActivePlayers()) {
            player.setCurrentBetFromPlayers(0);
        }
    }

    private void playRound() {
        bettingManager.setBlinds();
        bettingManager.startBettingRound(BettingManager.BettingPhase.PRE_FLOP);
        this.bettingPhase = BettingManager.BettingPhase.PRE_FLOP;
        if (isRoundComplete()) {
            resetBets();
            determineWinner();
            endRound();
            return;
        }

        table.dealFlop(deck);
        table.showBoard();
        bettingManager.startBettingRound(BettingManager.BettingPhase.FLOP);
        this.bettingPhase = BettingManager.BettingPhase.FLOP;
        if (isRoundComplete()) {
            resetBets();
            determineWinner();
            endRound();
            return;
        }

        table.dealTurn(deck);
        table.showBoard();
        bettingManager.startBettingRound(BettingManager.BettingPhase.TURN);
        this.bettingPhase = BettingManager.BettingPhase.TURN;
        if (isRoundComplete()) {
            resetBets();
            determineWinner();
            endRound();
            return;
        }

        table.dealRiver(deck);
        table.showBoard();
        bettingManager.startBettingRound(BettingManager.BettingPhase.RIVER);
        this.bettingPhase = BettingManager.BettingPhase.RIVER;

        determineWinner();
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        resetBets();
        endRound();
    }

    private boolean isRoundComplete() {
        this.activePlayers = new ArrayList<>();
        for (Player player : playerManager.getActivePlayers()) {
            if (!player.isFolded()) {
                activePlayers.add(player);
            }
        }
        return activePlayers.size() == 1;
    }

    private synchronized void determineWinner() {
        List<SidePot> sidePots = potManager.getSidePots();
        double mainPot = potManager.getMainPot();

        if (activePlayers.size() == 1) {
            Player winner = activePlayers.get(0);
            double totalPot = potManager.getTotalPot(); // Метод должен суммировать все сайд-поты
            winner.increaseBalance(totalPot);
            System.out.println("Everybody folded!");
            pokerServer.sendChatMessage("Everybody folded!");
            System.out.println(winner.getName() + " wins " + totalPot + "!");
            pokerServer.sendChatMessage(winner.getName() + " wins " + totalPot + "!");
            List<Integer> winnerIds = Arrays.asList(winner.getConnectionId());
            List<List<Card>> winningCardsList = Arrays.asList(Collections.emptyList());
            List<String> combinationNamesList = Arrays.asList("Won by Fold");

            WinnerInfo info = new WinnerInfo(
                winnerIds,
                winningCardsList,
                combinationNamesList,
                totalPot
            );
            server.sendToAllTCP(info);
            return;
        }

        if (bettingPhase.equals(BettingManager.BettingPhase.RIVER)) {
            // Вычисляем руки для всех оставшихся игроков
            List<Player> activePlayersList = new ArrayList<>();
            for (Player p : playerManager.getActivePlayers()) {
                if (!p.isFolded()) {
                    new HandCollector(p, table); // Собираем карты на руках
                    p.setTotalHand();
                    p.evaluateHand();
                    p.setCombination();
                    System.out.println(p.getName() + ": " + p.getCombination() + "; " + p.getNameofCombination() + ";");
                    pokerServer.sendChatMessage(p.getName() + ": " + p.getCombination() + "; " + p.getNameofCombination() + ";");
                    activePlayersList.add(p);
                }
            }

            // Распределяем сайд-поты
            for (SidePot sidePot : sidePots) {
                distributePot(sidePot);
            }

            // Распределяем основной банк
            distributeMainPot(activePlayersList, mainPot);
        }
    }

    /**
     * Метод для распределения выигрыша из сайд-пота
     */
    private void distributePot(SidePot sidePot) {
        if (sidePot.getAmount() <= 0) {
            return; // Пропускаем пустые сайд-поты
        }

        int maxHandValue = -1;
        for (Player p : sidePot.getEligiblePlayers()) {
            int handValue = p.getHandValue();
            if (handValue > maxHandValue) {
                maxHandValue = handValue;
            }
        }

        List<Player> winners = new ArrayList<>();
        for (Player p : sidePot.getEligiblePlayers()) {
            if (p.getHandValue() == maxHandValue) {
                winners.add(p);
            }
        }

        double splitPot = sidePot.getAmount() / winners.size();

        // Распределяем деньги
        for (Player winner : winners) {
            winner.increaseBalance(splitPot);
            System.out.println(winner.getName() + " wins side pot of " + splitPot);
        }
        // Готовим WinnerInfo для нескольких победителей
        List<Integer> winnerIds = new ArrayList<>();
        List<List<Card>> winningCardsList = new ArrayList<>();
        List<String> combinationNamesList = new ArrayList<>();

        for (Player winner : winners) {
            winnerIds.add(winner.getConnectionId());
            winningCardsList.add(winner.getCombination());
            combinationNamesList.add(winner.getNameofCombination());
        }

        WinnerInfo info = new WinnerInfo(
            winnerIds,
            winningCardsList,
            combinationNamesList,
            splitPot
        );

        server.sendToAllTCP(info);
    }


    /**
     * Метод для распределения выигрыша из основного банка
     */
    private void distributeMainPot(List<Player> activePlayers, double mainPot) {
        if (mainPot <= 0) {
            return; // Пропускаем пустые банки
        }
        int maxHandValue = -1;
        for (Player p : activePlayers) {
            int handValue = p.getHandValue();
            if (handValue > maxHandValue) {
                maxHandValue = handValue;
            }
        }

        List<Player> winners = new ArrayList<>();
        for (Player p : activePlayers) {
            if (p.getHandValue() == maxHandValue) {
                winners.add(p);
            }
        }

        double mainSplitPot = mainPot / winners.size();

        for (Player winner : winners) {
            winner.increaseBalance(mainSplitPot);
            System.out.println(winner.getName() + " wins main pot of " + mainSplitPot);
        }
    }

    private void endRound() {
        System.out.println("Ending round...");
        pokerServer.sendChatMessage("Ending round...");
        table.clearBoard(deck);
        playerManager.collectCards(deck);
        playerManager.removeBrokePlayers();
        for (Player player : playerManager.getActivePlayers()) {
            player.reset();
        }
        potManager.reset();
        if (playerManager.getActivePlayers().size() == 1) {
            System.out.println("Game Over. Winner: " + playerManager.getActivePlayers().get(0).getName());
            pokerServer.sendChatMessage("Game Over. Winner: " + playerManager.getActivePlayers().get(0).getName());
            Stop();
        }
    }

    private void Stop() {
        if (playerManager.getActivePlayers().size() == 1) {
            System.exit(0);
        }
    }
}
