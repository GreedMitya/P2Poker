package GreedMitya.P2Poker.core;

import GreedMitya.P2Poker.PacketsClasses.EndOfHandPacket;
import GreedMitya.P2Poker.PacketsClasses.Logger;

import GreedMitya.P2Poker.Server.PokerServer;
import com.esotericsoftware.kryonet.Server;

import java.util.*;

public class PokerGame {
    private PokerServer pokerServer;
    private final Set<Integer> readyPlayerIds = new HashSet<>();
    private Server server;
    public Deck deck;
    public Table table;
    public BettingManager bettingManager;
    public PlayerManager playerManager;
    private int roundCounter = 0;
    private List<Player> activePlayers;
    private BettingManager.BettingPhase bettingPhase;
    private PotManager potManager;

    private StartGame startGame;

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
        roundCounter = 0;
        startNextRound();
    }

    public void startNextRound() {
        synchronized(this) {
            if (playerManager.getActivePlayers().size() <= 1) {
                String winner = playerManager.getActivePlayers().get(0).getName();
                Logger.Game("Game Over. Winner: " + winner);
                pokerServer.sendChatMessage("Game Over. Winner: " + winner);
                pokerServer.sendWinnerAndShutdown(winner);
                return;
            }
            roundCounter++;
            Logger.Game("\nStarting Round: " + roundCounter);
            deck.shuffle();
            Logger.Game("Active players: " + playerManager.getActivePlayers().size());
            pokerServer.sendChatMessage("Hand number: " + roundCounter + ";");
            pokerServer.sendChatMessage("Active players: " + playerManager.getActivePlayers().size() + ";");
            playRound();
        }
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
        playerManager.prepareForRound(deck);
        bettingManager.startBettingRound(BettingManager.BettingPhase.PRE_FLOP);
        this.bettingPhase = BettingManager.BettingPhase.PRE_FLOP;
        if (isRoundComplete()) {
            resetBets();
            determineWinner();
            endRound();
            return;
        }
        pause(500);
        table.dealFlop(deck);
        table.showBoard();
        pokerServer.sendChatMessage("Flop: " + table.board.toString());
        bettingManager.startBettingRound(BettingManager.BettingPhase.FLOP);
        this.bettingPhase = BettingManager.BettingPhase.FLOP;
        if (isRoundComplete()) {
            resetBets();
            determineWinner();
            endRound();
            return;
        }
        pause(800);
        table.dealTurn(deck);
        table.showBoard();
        pokerServer.sendChatMessage("Turn: " + table.board.toString());
        bettingManager.startBettingRound(BettingManager.BettingPhase.TURN);
        this.bettingPhase = BettingManager.BettingPhase.TURN;
        if (isRoundComplete()) {
            resetBets();
            determineWinner();
            endRound();
            return;
        }
        pause(800);
        table.dealRiver(deck);
        table.showBoard();
        pokerServer.sendChatMessage("River: " + table.board.toString());
        bettingManager.startBettingRound(BettingManager.BettingPhase.RIVER);
        this.bettingPhase = BettingManager.BettingPhase.RIVER;

        determineWinner();
    }
    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
        double totalPot = potManager.getTotalPot();

        Map<Integer, Double> winningsByPlayerId = new HashMap<>();

        List<Player> alive = new ArrayList<>();
        for (Player p : playerManager.getActivePlayers()) {
            if (!p.isFolded()) alive.add(p);
        }
        if (alive.size() == 1) {
            Player sole = alive.get(0);
            sole.increaseBalance(totalPot);
            winningsByPlayerId.put(sole.getConnectionId(), totalPot);

            pokerServer.sendChatMessage("Everybody folded!");
            pokerServer.sendChatMessage(sole.getName() + " wins " + totalPot + "!");

            sendEndOfHandPacket(alive, totalPot, winningsByPlayerId);
            return;
        }

        if (bettingPhase.equals(BettingManager.BettingPhase.RIVER)) {
            List<Player> contenders = new ArrayList<>();
            for (Player p : playerManager.getActivePlayers()) {
                if (!p.isFolded()) {
                    new HandCollector(p, table);
                    p.setTotalHand();
                    p.evaluateHand();
                    p.setCombination();
                    pokerServer.sendChatMessage(
                       p.getName() + "'s combination: " + p.getNameofCombination());
                    contenders.add(p);
                }
            }

            for (SidePot sp : sidePots) {
                distributePot(sp, winningsByPlayerId);
            }
            distributeMainPot(contenders, mainPot, winningsByPlayerId);

            sendEndOfHandPacket(contenders, totalPot, winningsByPlayerId);
        }
    }


    private void sendEndOfHandPacket(
        List<Player> winners,
        double totalPot,
        Map<Integer, Double> winningsByPlayerId) {

        Map<Integer, List<Card>> handsByPlayerId = new HashMap<>();

        boolean isWinByFold = winners.size() == 1 && playerManager.getActivePlayers().size() > 1;

        for (Player player : playerManager.getActivePlayers()) {
            if (isWinByFold) {
                handsByPlayerId.put(player.getConnectionId(), new ArrayList<>());
            } else {
                handsByPlayerId.put(player.getConnectionId(), player.getHand());
            }
        }

        List<Integer> winnerIds = new ArrayList<>();
        List<List<Card>> winningCardsList = new ArrayList<>();
        List<String> combinationNamesList = new ArrayList<>();

        for (Player winner : winners) {
            List<Card> combo = winner.getCombination();

            if (combo == null || combo.size() != 5) {
                Logger.server("❌ ВНИМАНИЕ: Комбинация у победителя " + winner.getName() + " отсутствует или некорректна! combo=" + combo);
                combo = new ArrayList<>();
            }

            winnerIds.add(winner.getConnectionId());
            winningCardsList.add(combo);
            if (isWinByFold) {
                combinationNamesList.add("Fold Win");
            } else {
                combinationNamesList.add(winner.getNameofCombination() != null ? winner.getNameofCombination() : "No Combination");
            }
        }

        EndOfHandPacket packet = new EndOfHandPacket();
        packet.setHandsByPlayerId(handsByPlayerId);
        packet.setWinnerIds(winnerIds);
        packet.setWinningCards(winningCardsList);
        packet.setCombinationNames(combinationNamesList);
        packet.setAmountWon(totalPot);
        packet.setWinningsByPlayerId(winningsByPlayerId);

        server.sendToAllTCP(packet);
    }

    public void setStartGame(StartGame startGame) {
        this.startGame = startGame;
    }




    private void distributePot(SidePot sidePot, Map<Integer, Double> winningsMap) {
        if (sidePot.getAmount() <= 0) return;

        int best = sidePot.getEligiblePlayers().stream()
            .mapToInt(Player::getHandValue)
            .max()
            .orElse(-1);

        List<Player> winners = new ArrayList<>();
        for (Player p : sidePot.getEligiblePlayers()) {
            if (p.getHandValue() == best) winners.add(p);
        }

        double share = sidePot.getAmount() / winners.size();
        for (Player w : winners) {
            w.increaseBalance(share);
            winningsMap.merge(w.getConnectionId(), share, Double::sum);
        }
    }
    private void distributeMainPot(
        List<Player> contenders,
        double mainPot,
        Map<Integer, Double> winningsMap) {

        if (mainPot <= 0) return;

        int best = contenders.stream()
            .mapToInt(Player::getHandValue)
            .max()
            .orElse(-1);

        List<Player> winners = new ArrayList<>();
        for (Player p : contenders) {
            if (p.getHandValue() == best) winners.add(p);
        }

        double share = mainPot / winners.size();
        for (Player w : winners) {
            w.increaseBalance(share);
            winningsMap.merge(w.getConnectionId(), share, Double::sum);
        }
    }


    public void endRound() {
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
            pokerServer.sendWinnerAndShutdown(playerManager.getActivePlayers().get(0).getName());
            return;
        }
        playerManager.updatePlayersBalance();
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
