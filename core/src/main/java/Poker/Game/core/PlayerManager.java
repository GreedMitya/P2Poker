package Poker.Game.core;

import Poker.Game.PacketsClasses.CardInfo;

import java.util.ArrayList;

import Poker.Game.PacketsClasses.Logger;
import com.esotericsoftware.kryonet.Server;
import jdk.jpackage.internal.Log;

public class PlayerManager {
    private Server server;
    private ArrayList<Player> players;
    private ArrayList<Player> activePlayers;

    public PlayerManager(ArrayList<Player> players) {
        this.players = players;
        this.activePlayers = new ArrayList<>(players);
    }
    public PlayerManager(){

    }
    public void reloadActivePlayersList(){
        this.activePlayers = new ArrayList<>(players);
    }

    public ArrayList<Player> getActivePlayers() {
        return activePlayers;
    }
    public ArrayList<Player> getPlayers() {
        return players;
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

        for (Player player : players) {
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
        players.forEach(player -> {
            player.getHand().forEach(deck::returnCard);
            player.clearHand();
        });
    }

    public void removeBrokePlayers() {
        activePlayers.removeIf(player -> player.getBalance() == 0);
    }

    public void setServer(Server server) {
        this.server = server;
    }
}
