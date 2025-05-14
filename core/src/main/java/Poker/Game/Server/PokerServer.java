package Poker.Game.Server;

import Poker.Game.PacketsClasses.*;
import Poker.Game.core.Player;
import Poker.Game.core.PokerGame;
import Poker.Game.core.StartGame;
import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class PokerServer {
    private final Map<Integer, Boolean> playerReadyStatus = new HashMap<>();
    private Server server;
    private StartGame startGame;

    // Храним nickname по connectionId
    private final Map<String,Integer> playerNicknames = new HashMap<>();

    // pendingActions: ключ — connectionId игрока, значение — будущее выбранного действия
    private final Map<Integer, CompletableFuture<PlayerAction>> pendingActions = new ConcurrentHashMap<>();


    // Служба для таймаутов (Java 8)
    private final ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(4);

    public void start() throws IOException {
        server = new Server();
        // Регистрируем все пакеты (ActionRequest, ActionResponse, Action, Action[].class и т.д.)
        Network.register(server.getKryo());

        // Запускаем «слушалку» для любых UDP Broadcast, если нужно
        BroadcastResponder.startListening();

        // Listener для основных пакетов: Join, GameStart и наши ActionResponse
        server.addListener(new Listener() {
            @Override
            public void connected(Connection conn) {
                Logger.server("connID=" + conn.getID() + " connected from " + conn.getRemoteAddressTCP());
            }


            @Override
            public void disconnected(Connection connection) {
                Gdx.app.log("Server", "Client disconnected (id=" + connection.getID() + ")");
                // Проверим, остались ли ещё активные клиенты
                if (server.getConnections().size() == 0) {
                    Gdx.app.log("Server", "No more clients, stopping server…");
                    // Остановим сервер: закроем сокеты и потоки
                    server.stop();
                    // Если хотим — завершаем программу целиком
                    //System.exit(0);
                }
            }

            @Override
            public void received(Connection conn, Object obj) {
                Logger.server("⧗ Received packet of type: " + obj.getClass().getName());


                if (obj instanceof JoinRequest) {
                    JoinRequest req = (JoinRequest) obj;
                    Logger.server("Player connected " + req.nickname);
                    playerNicknames.put(req.nickname,conn.getID());

                    Player player = new Player(req.nickname);
                    startGame.addPlayer(player.getName(), conn.getID());

                    server.sendToAllTCP(new PlayerJoinedNotification(req.nickname,conn.getID()));
                    server.sendToAllTCP(new PlayerListUpdate(playerNicknames));


                } else if (obj instanceof ActionResponse) {
                    ActionResponse resp = (ActionResponse) obj;
                    Logger.server("📥 [RESP] от playerId=" + resp.playerId +
                        " action=" + resp.chosenAction.name + ", amount=" + resp.amount);

                    CompletableFuture<PlayerAction> future = pendingActions.remove(resp.playerId);
                    if (future != null) {
                        PlayerAction action = new PlayerAction(resp.chosenAction.name, resp.amount);
                        future.complete(action);
                    } else {
                        Logger.server("‼ Нет pendingActions для " + resp.playerId);
                    }
                } else if (obj instanceof GameStartRequest) {
                    if (conn.getID() == 1 && startGame.getPlayers().size() >= 2) {
                        server.sendToAllTCP(new GameStartedNotification());
                        Logger.server("Game started by host");
                        new Thread(() -> {
                            try {
                                startGame.startGame(); // основной запуск игры
                            } catch (Exception e) {
                                Gdx.app.error("Server", "Ошибка при запуске игры: " + e.getMessage(), e);
                                server.stop(); // остановим сервер, если что-то пошло не так
                                System.exit(1); // опционально, если хочешь завершить JVM
                            }
                        }).start();
                    } else {
                        Logger.server("Нельзя запустить игру: не хост или мало игроков");
                    }
                } else if (obj instanceof ChatMessage) {
                    ChatMessage mess = (ChatMessage) obj;
                    server.sendToAllTCP(mess);
                }else if (obj instanceof ClientReadyForNextRound) {
                    handleClientReadyForNextRound((ClientReadyForNextRound) obj);
                } else if (obj instanceof RestartGameRequest) {
                    RestartGameRequest req = (RestartGameRequest) obj;
                    if (req.senderId == 1) { // Только хост
                        Logger.server("↻ Получен Restart от хоста!");

                        // 1. Обнулим всё
                        playerNicknames.clear();
                        pendingActions.clear();

                        // 2. Перезапустим логику StartGame
                        startGame = new StartGame();
                        startGame.setServer(PokerServer.this);

                        // 3. Всем клиентам: сказать, что произошёл рестарт
                        server.sendToAllTCP(new ChatMessage("♻ Игра была перезапущена хостом", "sys"));
                        server.sendToAllTCP(new PlayerListUpdate(playerNicknames));
                        server.sendToAllTCP(new RestartGameNotification());
                    }
                }

            }
        });

        server.bind(54555, 54777);  // выбирайте ваши порты
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Gdx.app.log("Server", "JVM shutdown hook: stopping server…");
            server.stop();
        }));


        // KeepAlive-пинг всем клиентам
        scheduler.scheduleAtFixedRate(() -> {
            for (Connection conn : server.getConnections()) {
                conn.sendTCP(new KeepAlive());
            }
            //Logger.server("KeepAlive отправлен всем клиентам");
        }, 5, 5, TimeUnit.SECONDS);

        // Инициализируем логику игры
        startGame = new StartGame();
        startGame.setServer(this);

        Logger.server("Сервер запущен и ждёт подключений...");
    }

    /**
     * Запрос хода у клиента.
     * @param connectionId ID соединения (Connection.getID())
     * @param availableActions массив доступных Action
     * @param timeoutSec таймаут в секундах
     * @return Future, которое вернёт name выбранного действия
     */
    public CompletableFuture<PlayerAction> requestPlayerAction(int connectionId,
                                                               Action[] availableActions,
                                                               int timeoutSec) {
        CompletableFuture<PlayerAction> future = new CompletableFuture<>();
        pendingActions.put(connectionId, future);

        ActionRequest req = new ActionRequest();
        req.playerId = connectionId;
        req.availableActions = availableActions;
        req.timeoutSec = timeoutSec;
        server.sendToTCP(connectionId, req);

        Logger.server("► ActionRequest sent to playerId=" + connectionId +
            ", actions=" + Arrays.toString(availableActions) +
            ", timeout=" + timeoutSec);

        // Таймаут → auto-fold
        scheduler.schedule(() -> {
            if (!future.isDone()) {
                Logger.server("Timeout: auto-fold for " + connectionId);
                future.complete(new PlayerAction("fold", 0));
                pendingActions.remove(connectionId);
            }
        }, timeoutSec, TimeUnit.SECONDS);

        return future;
    }
    public void sendChatMessage(String text) {
        String name = "sys";
        server.sendToAllTCP(new ChatMessage(text, name));
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }
    private void handleClientReadyForNextRound(ClientReadyForNextRound packet) {
        int playerId = packet.getPlayerId();
        boolean isReady = packet.isReady();
        playerReadyStatus.put(playerId, isReady);

        // Проверяем, все ли игроки готовы
        if (areAllPlayersReady()) {
            startNextRound();
        }
    }
    private boolean areAllPlayersReady() {
        for (Boolean isReady : playerReadyStatus.values()) {
            if (!isReady) {
                return false;
            }
        }
        return true;
    }
    private void startNextRound() {
        // Логика для начала нового раунда
        System.out.println("Все игроки готовы. Начинаем новый раунд!");
        startGame.getGame().startNextRound();
    }
}
