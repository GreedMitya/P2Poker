package GreedMitya.P2Poker.core;

import GreedMitya.P2Poker.PacketsClasses.CardInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import GreedMitya.P2Poker.PacketsClasses.Logger;
import GreedMitya.P2Poker.PacketsClasses.PlayerBalanceUpdate;
import GreedMitya.P2Poker.PacketsClasses.ReturnToLobbyPacket;
import GreedMitya.P2Poker.Server.PokerServer;
import com.esotericsoftware.kryonet.Server;

public class PlayerManager {
    private Server server;
    private ArrayList<Player> players;
    private ArrayList<Player> activePlayers;
    private PokerServer pokerServer;

    public PlayerManager(ArrayList<Player> players) {
        this.players = players;
        this.activePlayers = new ArrayList<>(players);
    }
    public PlayerManager(){

    }
    public void reloadActivePlayersList(){
        if (players == null) {
            Logger.Game("Список всех игроков не задан.");
            return;
        }

        activePlayers = new ArrayList<>();
        for (Player player : players) {
            if (player != null && player.getBalance() > 0) {
                activePlayers.add(player);
            }
        }

        Logger.Game("Обновлён список активных игроков: " + activePlayers.size());
        for (Player p : activePlayers) {
            Logger.Game(" -> " + p.getName() + " с балансом " + p.getBalance());
        }
    }


    public ArrayList<Player> getActivePlayers() {
        return activePlayers;
    }
    public ArrayList<Player> getPlayers() {
        return players;
    }
    public void addPlayer(Player player){
        players.add(player);
    }


    public void prepareForRound(Deck deck) {
        updatePlayersBalance();
        if (players == null || players.isEmpty()) {
            Logger.Game("Список игроков не инициализирован.");
            return;
        }

        if (activePlayers == null || activePlayers.isEmpty()) {
            Logger.Game("Список активных игроков не инициализирован.");
            return;
        }

        activePlayers.forEach(player -> {
            if (player != null) {
                player.drawCards(deck);
            } else {
                Logger.Game("Игрок в списке активных игроков равен null.");
            }
        });

        for (Player player : activePlayers) {
            if (player != null) {
                CardInfo cardMessage = new CardInfo(player.getHand());
                if (server != null) {
                    server.sendToTCP(player.getConnectionId(), cardMessage);
                } else {
                    Logger.Game("Сервер не инициализирован.");
                }
            } else {
                Logger.Game("Игрок в списке игроков равен null.");
            }
        }
    activePlayers.forEach(player -> Logger.Game(player.getName() + ": " + " [" +player.getBalance() + "]-" + player.getHand()));
    }
    public void updatePlayersBalance(){
        for (Player player : activePlayers) {
            if (player != null) {
                PlayerBalanceUpdate upd = new PlayerBalanceUpdate();
                upd.name = player.getName();
                upd.newBalance = player.getBalance();
                server.sendToAllTCP(upd);
            }
        }
    }

    public void collectCards(Deck deck) {
        activePlayers.forEach(player -> {
            player.getHand().forEach(deck::returnCard);
            player.clearHand();
        });
    }

    public void removeBrokePlayers() {
        List<Player> losers = activePlayers.stream()
            .filter(p -> p.getBalance() == 0)
            .collect(Collectors.toList());
        if (losers.isEmpty()) {
            return;
        }

        activePlayers.removeAll(losers);

        for (Player loser : losers) {
            server.sendToTCP(loser.getConnectionId(), new ReturnToLobbyPacket());
            pokerServer.playerReadyStatus.remove(loser.getConnectionId());
        }
    }

    public void setPokerServer(PokerServer pokerServer) {
        this.pokerServer = pokerServer;
    }

    public void setServer(Server server) {
        this.server = server;
    }
}
