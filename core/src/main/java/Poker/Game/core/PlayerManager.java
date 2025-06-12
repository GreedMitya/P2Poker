package Poker.Game.core;

import Poker.Game.PacketsClasses.CardInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import Poker.Game.PacketsClasses.Logger;
import Poker.Game.PacketsClasses.ReturnToLobbyPacket;
import Poker.Game.Server.PokerServer;
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

    public void collectCards(Deck deck) {
        activePlayers.forEach(player -> {
            player.getHand().forEach(deck::returnCard);
            player.clearHand();
        });
    }

    public void removeBrokePlayers() {
        // Собираем всех банкротов
        List<Player> losers = activePlayers.stream()
            .filter(p -> p.getBalance() == 0)
            .collect(Collectors.toList());
        if (losers.isEmpty()) {
            return; // никого не нужно удалять
        }

        // Удаляем их из активных
        activePlayers.removeAll(losers);

        // Шлём каждому ReturnToLobbyPacket
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
