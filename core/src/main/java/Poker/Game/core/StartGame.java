package Poker.Game.core;

import Poker.Game.PacketsClasses.Logger;
import Poker.Game.PacketsClasses.PlayerBalanceUpdate;
import Poker.Game.Server.PokerServer;
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


    public void addPlayer(String nickname, int connectionID) {
        Player newPlayer = new Player(nickname);
        newPlayer.setServer(server);
        newPlayer.setConnectionId(connectionID);// для связи по сети
        players.add(newPlayer);
        Logger.Game("Добавлен игрок: " + nickname);
        PlayerBalanceUpdate upd = new PlayerBalanceUpdate();
        upd.name = newPlayer.getName();
        upd.newBalance = newPlayer.getBalance();
        server.sendToAllTCP(upd);
    }

    public void startGame() {
        if (players.size() >= 2) {
            game = new PokerGame(players);// ✅ сначала создаём игру
            game.setPokerServer(pokerServer);
            game.setServer(server);
            game.bettingManager.setPokerServer(pokerServer);
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

