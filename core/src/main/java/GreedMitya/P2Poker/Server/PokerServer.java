package GreedMitya.P2Poker.Server;
import GreedMitya.P2Poker.PacketsClasses.*;
import GreedMitya.P2Poker.core.Player;
import GreedMitya.P2Poker.core.StartGame;
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
    public final Map<String, Integer> playerNicknames = new ConcurrentHashMap<>();
    private final Map<Integer, CompletableFuture<PlayerAction>> pendingActions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

    public PokerServer() {
        instance = this;
    }

    public void start() throws IOException {
        server = new Server();

        Network.register(server.getKryo());

        BroadcastResponder.startListening();

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

                playerNicknames.entrySet().removeIf(entry -> entry.getValue().equals(id));
                startGame.removePlayer(id);
                //server.sendToAllTCP(new PlayerListUpdate(playerNicknames));

                if (id == 1) {
                    Logger.server("Хост отключился! Сервер будет остановлен...");
                    sendChatMessage(" Хост покинул игру. Сервер завершает работу...");
                    shutdownServer();
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
                            } catch (Exception e) {
                                Logger.server("Ошибка при запуске игры: " + e.getMessage());
                                e.printStackTrace();
                                server.stop();
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
                    if (req.senderId == 1) {
                        Logger.server("Получен Restart от хоста!");
                        playerNicknames.clear();
                        pendingActions.clear();
                        startGame = new StartGame();
                        startGame.setServer(PokerServer.this);
                        server.sendToAllTCP(new ChatMessage("Игра была перезапущена хостом", "sys"));
                        server.sendToAllTCP(new PlayerListUpdate(playerNicknames));
                        server.sendToAllTCP(new RestartGameNotification());
                    }
                }

            }
        });

        bindWithRetry(54555, 54777);
        server.start();


        scheduler.scheduleWithFixedDelay(() -> {
            for (Connection conn : server.getConnections()) {
                conn.sendTCP(new KeepAlive());
            }
            //Logger.server("KeepAlive отправлен всем клиентам");
        }, 5, 5, TimeUnit.SECONDS);

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
                return;
            } catch (BindException e) {
                attempts++;
                if (attempts >= maxAttempts) {
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

                if (intf.isLoopback() || !intf.isUp()) continue;

                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();

                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        String ip = inetAddress.getHostAddress();
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
                server.sendToTCP(id,new PlayerListUpdate(playerNicknames));
            } else {
                Player player = new Player(req.nickname);
                startGame.addPlayer(player.getName(), id);
                server.sendToAllTCP(new PlayerJoinedNotification(req.nickname, id));
                server.sendToAllTCP(new PlayerListUpdate(playerNicknames));
            }
        }
    }

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
            for (Integer id : playerReadyStatus.keySet()) {
                playerReadyStatus.put(id, false);
            }
            scheduler.schedule(() -> {
                //Logger.server(" 1 секунда истекла – начинаем новый раунд");
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
        for (Map.Entry<String, Integer> entry : waitingPlayers.entrySet()) {
            String nickname = entry.getKey();
            int connectionID = entry.getValue();
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
        //System.out.println("Все игроки готовы. Начинаем новый раунд!");
        startGame.getGame().resetBets();
        startGame.getGame().endRound();
        startGame.getGame().startNextRound();
    }


    public void sendWinnerAndShutdown(String winnerName) {
        if (isShuttingDown) return;
        isShuttingDown = true;
        WinnerPacket win = new WinnerPacket();
        win.setName(winnerName);
        server.sendToAllTCP(win);
        scheduler.schedule(() -> {
            server.sendToAllTCP(new ReturnToLobbyPacket());
            scheduler.schedule(this::shutdownServer, 1, TimeUnit.SECONDS);

        }, 5, TimeUnit.SECONDS);
    }
    public void shutdownServer() {
        System.out.println("[SERVER] Shutting down...");
        BroadcastResponder.stopListening();
        scheduler.shutdownNow();
        try {
            scheduler.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
        server.stop();
        server.close();
        instance = null;
    }
}
