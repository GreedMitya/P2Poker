package GreedMitya.P2Poker.core;

import GreedMitya.P2Poker.PacketsClasses.Logger;
import GreedMitya.P2Poker.PacketsClasses.PlayerBalanceUpdate;
import GreedMitya.P2Poker.Server.PokerServer;
import com.esotericsoftware.kryonet.Server;

import java.util.ArrayList;

public class StartGame {
    private PokerServer pokerServer;
    private Server server;
    private ArrayList<Player> players; // теперь поле, а не внутри main
    private PokerGame game;

    public StartGame() {
        players = new ArrayList<>();
    }

    public void setServer(PokerServer pokerServer){
        this.pokerServer = pokerServer;
        this.server = pokerServer.getServer();
    }
    public void removePlayer(int connectionID) {
        Player toRemove = null;

        for (Player p : players) {
            if (p.getConnectionId() == connectionID) {
                toRemove = p;
                break;
            }
        }

        if (toRemove != null) {
            players.remove(toRemove);
            Logger.Game("Удалён игрок: " + toRemove.getName());
        } else {
            Logger.Game("⚠ Не найден игрок с connectionId=" + connectionID);
        }

        if (game != null && game.playerManager != null) {
            game.playerManager.reloadActivePlayersList();
        }
    }



    public void addPlayer(String nickname, int connectionID) {
        synchronized (this) {
            // Проверка дубликатов
            for (Player p : players) {
                if (p.getConnectionId() == connectionID) {
                    Logger.Game("Игрок с connectionID " + connectionID + " уже добавлен, пропускаем.");
                    return;
                }
                if (p.getName().equals(nickname)) {
                    Logger.Game("Игрок с ником " + nickname + " уже добавлен, пропускаем.");
                    return;
                }
            }
            // Если прошло проверку — добавляем
            Player newPlayer = new Player(nickname);
            newPlayer.setServer(server);
            newPlayer.setConnectionId(connectionID);

            players.add(newPlayer);

            Logger.Game("Добавлен игрок: " + nickname + (pokerServer.gameAlreadyStarted ? " (ожидает следующей раздачи)" : ""));

            PlayerBalanceUpdate upd = new PlayerBalanceUpdate();
            upd.name = newPlayer.getName();
            upd.newBalance = newPlayer.getBalance();
            server.sendToAllTCP(upd);
        }
    }



    public void startGame() {
        if (players.size() >= 2) {
            pokerServer.gameAlreadyStarted = true;
            game = new PokerGame(players);// ✅ сначала создаём игру
            game.setStartGame(this);
            game.setPokerServer(pokerServer);
            game.setServer(server);
            game.bettingManager.setPokerServer(pokerServer);
            game.playerManager.setPokerServer(pokerServer);// ✅ теперь можно обращаться к её полям
            game.playerManager.setServer(server);// ✅ теперь можно обращаться к её полям
            game.table.setServer(server);
            game.startGame(); // 🚀 запускаем игру
        } else {
            Logger.Game("Недостаточно игроков для начала");
        }
    }


    public ArrayList<Player> getPlayers() {
        return players;
    }

    public int getConnectionIdByPlayerId(Player playerId) {
        return playerId.getConnectionId();
    }
    public PokerGame getGame(){
        return game;
    }

    // main не нужен — запуск будет с UI или сетевого события
}

