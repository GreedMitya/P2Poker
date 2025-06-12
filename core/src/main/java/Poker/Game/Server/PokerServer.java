package Poker.Game.Server;

import Poker.Game.PacketsClasses.*;
import Poker.Game.core.Player;
import Poker.Game.core.StartGame;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class PokerServer {
    private static PokerServer instance;
    public volatile boolean gameAlreadyStarted = false;
    protected final Map<String, Integer> waitingPlayers = new ConcurrentHashMap<>();

    private final Set<Integer> activeConnections = ConcurrentHashMap.newKeySet();

    public final Map<Integer, Boolean> playerReadyStatus = new ConcurrentHashMap<>();

    private Server server;
    private StartGame startGame;
    private volatile boolean isShuttingDown = false;
    // Храним nickname по connectionId
    public final Map<String, Integer> playerNicknames = new ConcurrentHashMap<>();
    // pendingActions: ключ — connectionId игрока, значение — будущее выбранного действия
    private final Map<Integer, CompletableFuture<PlayerAction>> pendingActions = new ConcurrentHashMap<>();

    // вместо Executors.newScheduledThreadPool(4);
    private final ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);  // демон — не будет мешать JVM завершиться
            return t;
        });

    public PokerServer() {
        instance = this;
    }

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
                //Logger.server("connID=" + conn.getID() + " connected from " + conn.getRemoteAddressTCP());
                activeConnections.add(conn.getID());
            }


            @Override
            public void disconnected(Connection conn) {
                int id = conn.getID();
                //Logger.server("connID=" + id + " disconnected.");
                activeConnections.remove(id);
                playerReadyStatus.remove(id);
                pendingActions.remove(id);

                // Удаляем никнейм по значению (через итерацию)
                playerNicknames.entrySet().removeIf(entry -> entry.getValue().equals(id));
                startGame.removePlayer(id);
                server.sendToAllTCP(new PlayerListUpdate(playerNicknames));

                // ⛔ Если хост отключился — завершить сервер
                if (id == 1) {
                    Logger.server("⛔ Хост отключился! Сервер будет остановлен...");
                    sendChatMessage("⛔ Хост покинул игру. Сервер завершает работу...");
                    shutdownServer();// 2 сек задержка для красоты
                }
            }






            @Override
            public void received(Connection conn, Object obj) {
                //Logger.server("⧗ Received packet of type: " + obj.getClass().getName());


                if (obj instanceof JoinRequest) {
                    JoinRequest req = (JoinRequest) obj;
                    int connId = conn.getID();
                    handleJoin(req, connId);
                    if(connId==1){
                        sendChatMessage("Server ip: " + getLocalIpAddress());
                        sendChatMessage("Ports: 54555/54777");
                    }
                } else if (obj instanceof ActionResponse) {
                    ActionResponse resp = (ActionResponse) obj;
                    //Logger.server("📥 [RESP] от playerId=" + resp.playerId +
                    //    " action=" + resp.chosenAction.name + ", amount=" + resp.amount);

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
                        Thread game = new Thread(() -> {
                            try {
                                startGame.startGame();
                                // основной запуск игры
                            } catch (Exception e) {
                                Logger.server("Ошибка при запуске игры: " + e.getMessage());
                                e.printStackTrace(); // или Logger.server(e)
                                server.stop(); // остановим сервер
                            }

                        });game.setDaemon(true);game.start();
                    } else {
                        Logger.server("Нельзя запустить игру: не хост или мало игроков");
                    }
                } else if (obj instanceof ChatMessage) {
                    ChatMessage mess = (ChatMessage) obj;
                    server.sendToAllTCP(mess);
                }else if (obj instanceof ClientReadyForNextRound) {
                    //Logger.server("📥 [RESP] Client ready packet from playerId=" + ((ClientReadyForNextRound) obj).getPlayerId());
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

        bindWithRetry(54555, 54777);// выбирайте ваши порты
        server.start();


        // KeepAlive-пинг всем клиентам
        scheduler.scheduleWithFixedDelay(() -> {
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
    private void bindWithRetry(int tcpPort, int udpPort) throws IOException {
        int attempts = 0;
        final int maxAttempts = 5;
        final long delayMs = 200;

        while (true) {
            try {
                server.bind(tcpPort, udpPort);
                return; // успешно забиндились — выходим
            } catch (BindException e) {
                attempts++;
                if (attempts >= maxAttempts) {
                    // не смогли за maxAttempts — пробрасываем
                    throw new IOException(
                        "Не удалось забиндить порты " + tcpPort + "/" + udpPort +
                            " после " + attempts + " попыток", e);
                }
                Logger.server("Порт занят, retry " + attempts + "/" + maxAttempts +
                    " через " + delayMs + "ms...");
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Ожидание между попытками бинда прервано", ie);
                }
            }
        }
    }

    public static PokerServer getInstance() {
        return instance;
    }
    private boolean isNicknameTaken(String nickname) {
        return playerNicknames.containsKey(nickname) || waitingPlayers.containsKey(nickname);
    }
    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();

                // Пропускаем loopback и неактивные
                if (intf.isLoopback() || !intf.isUp()) continue;

                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();

                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        String ip = inetAddress.getHostAddress();
                        // Фильтрация виртуальных/мобильных
                        if (!ip.startsWith("127.") && !ip.startsWith("0.")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return "Unknown";
    }


    private void handleJoin(JoinRequest req, int id) {
        synchronized(this) {
            if (isNicknameTaken(req.nickname)) {
                sendChatMessage("Ник " + req.nickname + " уже занят!");
                return;
            }
            playerNicknames.put(req.nickname, id);
            playerReadyStatus.put(id, true);

            if (gameAlreadyStarted) {
                waitingPlayers.put(req.nickname, id);
                sendChatMessage(req.nickname + " connected! Will play from next round!");
            } else {
                Player player = new Player(req.nickname);
                startGame.addPlayer(player.getName(), id);
                server.sendToAllTCP(new PlayerJoinedNotification(req.nickname, id));
                server.sendToAllTCP(new PlayerListUpdate(playerNicknames));
            }
        }
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
                                                               int timeoutSec,
                                                               double playerCurrentBet,
                                                               double currentBet) {
        CompletableFuture<PlayerAction> future = new CompletableFuture<>();
        pendingActions.put(connectionId, future);

        ActionRequest req = new ActionRequest();
        req.playerId = connectionId;
        req.availableActions = availableActions;
        req.timeoutSec = timeoutSec;
        server.sendToTCP(connectionId, req);

        //Logger.server("► ActionRequest sent to playerId=" + connectionId +
        //    ", actions=" + Arrays.toString(availableActions) +
        //    ", timeout=" + timeoutSec);

        // Таймаут → auto-check или auto-fold
        scheduler.schedule(() -> {
            if (!future.isDone()) {
                Logger.server("Timeout: auto-decision for " + connectionId);
                String action = (playerCurrentBet == currentBet) ? "check" : "fold";
                future.complete(new PlayerAction(action, 0));
                pendingActions.remove(connectionId);
            }
        }, timeoutSec, TimeUnit.SECONDS);

        return future;
    }

    public Map<String, Integer> getWaitingPlayers() {
        return waitingPlayers;
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

        if (areAllPlayersReady()) {
            //Logger.server("Все игроки подтвердили готовность – через 1 секунда стартуем новый раунд");
            // Сразу обнуляем, чтобы дополнительная обработка не запустила ещё один раунд
            for (Integer id : playerReadyStatus.keySet()) {
                playerReadyStatus.put(id, false);
            }
            // Планируем старт
            scheduler.schedule(() -> {
                //Logger.server("⏱ 1 секунда истекла – начинаем новый раунд");
                startNextRound();
            }, 2, TimeUnit.SECONDS);
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
        // Перед стартом нового раунда:
        for (Map.Entry<String, Integer> entry : waitingPlayers.entrySet()) {
            String nickname = entry.getKey();
            int connectionID = entry.getValue();
            // Проверить, нет ли такого игрока в active players
            boolean alreadyExists = false;
            for (Player p : startGame.getPlayers()) {
                if (p.getName().equals(nickname)) {
                    alreadyExists = true;
                    break;
                }
            }
            if (!alreadyExists) {
                startGame.addPlayer(nickname, connectionID);
                startGame.getGame().playerManager.reloadActivePlayersList();
            }
        }
        waitingPlayers.clear();
        server.sendToAllTCP(new PlayerListUpdate(playerNicknames));
        // 🔄 Очищаем список ожидающих
        // ✅ Обновляем списки для клиентов уже после добавления новых игроков
        //System.out.println("Все игроки готовы. Начинаем новый раунд!");
        startGame.getGame().resetBets();
        startGame.getGame().endRound();
        startGame.getGame().startNextRound();
    }


    public void sendWinnerAndShutdown(String winnerName) {
        if (isShuttingDown) return;
        isShuttingDown = true;

        // 1) Мгновенно рассылаем победителем
        WinnerPacket win = new WinnerPacket();
        win.setName(winnerName);
        server.sendToAllTCP(win);

        // 2) Через 5 сек — возвращаем всех в лобби
        scheduler.schedule(() -> {
            server.sendToAllTCP(new ReturnToLobbyPacket());

            // 3) Ещё через 1 сек — останавливаем сервер
            scheduler.schedule(this::shutdownServer, 1, TimeUnit.SECONDS);

        }, 5, TimeUnit.SECONDS);
    }
    // Удаляем старый sendWinner(!) и в core-коде вызываем именно sendWinnerAndShutdown(...)
    public void shutdownServer() {
        System.out.println("[SERVER] Shutting down...");
        BroadcastResponder.stopListening();


        // 1) Останавливаем таймеры
        scheduler.shutdownNow();
        try {
            scheduler.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
        // 2) Останавливаем KryoNet
        server.stop();
        server.close();
        instance = null;
    }
}
