package GreedMitya.P2Poker.Client;

import GreedMitya.P2Poker.*;
import GreedMitya.P2Poker.PacketsClasses.*;
import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.badlogic.gdx.Gdx.app;

public class PokerClient {
    private PokerApp pokerApp;
    private ChatListener chatListener;
    private int clientId;
    private Client client;
    private ClientListener listener;
    private boolean host;
    private String name;
    private final Map<Integer, String> idToNickname   = new HashMap<>();
    private final Map<String, Integer> nicknameToId   = new HashMap<>();

    public void onChatMessage(ChatListener listener) {
        this.chatListener = listener;
    }


    public PokerClient(PokerApp pokerApp) {
        this.pokerApp = pokerApp;
    }
    public PokerClient(){}
    public void registerPlayer(int id, String nickname) {
        idToNickname.put(id, nickname);
        nicknameToId.put(nickname, id);

        if (nickname.equals(this.name)) {
            this.clientId = id;
        }
    }

    public String getNicknameById(int id) {
        return idToNickname.get(id);
    }

    public int getIdByNickname(String name) {
        return nicknameToId.getOrDefault(name, -1);
    }


    public void setListener(ClientListener listener) {
        this.listener = listener;
    }

    public void start(String hostIP, String nickname) throws IOException {
        client = new Client();
        Kryo kryo = client.getKryo();
        Network.register(kryo);
        client.start();



        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Uncaught exception in thread " + thread.getName());
            throwable.printStackTrace();
        });


        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (listener == null) return;


                else if (object instanceof PlayerListUpdate) {
                    PlayerListUpdate update = (PlayerListUpdate) object;
                    for (Map.Entry<String, Integer> entry : update.getNicknames().entrySet()) {
                        String nickname = entry.getKey();
                        int playerId = entry.getValue();
                        registerPlayer(playerId, nickname);
                    }
                    listener.onPlayerListUpdate(update.getNicknamesOnly());
                }
                else if (object instanceof GameStartedNotification) {
                    chatListener.onGameStarted((GameStartedNotification) object);
                }else if(object instanceof PlayerJoinedNotification){
                    chatListener.onPlayerJoinedNotification((PlayerJoinedNotification) object);
                }else if (object instanceof CardInfo) {
                    listener.onCardInfo((CardInfo) object);
                } else if (object instanceof BlindsNotification) {
                    listener.onBlinds((BlindsNotification) object);
                } else if (object instanceof ActionRequest) {
                    //System.out.println("ClientListener"+ "Получен ActionRequest: " + object);
                    listener.onActionRequest((ActionRequest) object);
                } else if (object instanceof PlayerBalanceUpdate) {
                    listener.onPlayerBalanceUpdate((PlayerBalanceUpdate) object);
                } else if (object instanceof TableCardsInfo) {
                    listener.onTableCardsInfo((TableCardsInfo) object);
                } else if (object instanceof PlayerBetUpdate) {
                    listener.onPlayerBetUpdate((PlayerBetUpdate) object);
                }else if (object instanceof ChatMessage) {
                    ChatMessage message = (ChatMessage) object;
                    if (chatListener != null) {
                        chatListener.onChatMessage(message);
                    }
                }else if (object instanceof PotUpdate) {
                    listener.onPotUpdate((PotUpdate) object);
                }else if (object instanceof FoldNotification) {
                    listener.onPlayerFold((FoldNotification) object);
                }else if(object instanceof EndOfHandPacket){
                    listener.onEndOfHandPacket((EndOfHandPacket) object);
                }else if (object instanceof RestartGameNotification) {
                    listener.onGameRestart();
                }else if (object instanceof PlayerOrderPacket) {
                    listener.onPlayerOrderPacket((PlayerOrderPacket) object);
                }else if( object instanceof BetUpdatePack){
                    listener.onBetUpdatePack((BetUpdatePack) object);
                }else if (object instanceof ReturnToLobbyPacket) {
                    app.postRunnable(() -> {
                        if(!(connection.getID() == 1)) {
                            Logger.client("Возврат на стартовый экран");
                            disconnect();
                        }else{
                            sendChatMessage("Host loose the game need to restart for continue!))");
                        }
                        //pokerApp.setScreen(new LobbyScreen(pokerApp));
                    });
                }else if (object instanceof WinnerPacket){
                    onWinnerPacket((WinnerPacket) object);
                }else if (object instanceof SpectatorJoinedNotification) {
                    SpectatorJoinedNotification notif = (SpectatorJoinedNotification) object;
                }else if(object instanceof CheckPacket){
                    listener.onCheckPacket();
                }

            }

            public void disconnected(Connection connection) {
                Logger.client("Disconnected from server, returning to lobby…");
                client.stop();
                app.postRunnable(() -> {
                    pokerApp.setScreen(new LobbyScreen(pokerApp));
                });
            }
        });

        client.connect(5000, hostIP, 54555, 54777);

        this.name = nickname;
        client.sendTCP(new JoinRequest(nickname));
    }

    public void disconnect() {
        if (client != null && client.isConnected()) {
            idToNickname.clear();
            nicknameToId.clear();
            clientId = -1;
            name = null;
            client.close();
        }
    }
    public void sendCheck(int playerId) {
       // System.out.println("Отправляю действие: " + "Check" + " от игрока: " + playerId);
        ActionResponse resp = new ActionResponse();
        resp.playerId = playerId;
        resp.chosenAction = new Action("check", 0, 0, 0);
        client.sendTCP(resp);
    }

    public void sendFold(int playerId) {
        //System.out.println("Отправляю действие: " + "Fold" + " от игрока: " + playerId);
        ActionResponse resp = new ActionResponse();
        resp.playerId = playerId;
        resp.chosenAction = new Action("fold", 0, 0, 0);
        client.sendTCP(resp);
    }

    public void sendCall(int playerId) {
        //System.out.println("Отправляю действие: " + "Call" + " от игрока: " + playerId);

        ActionResponse resp = new ActionResponse();
        resp.playerId = playerId;
        resp.chosenAction = new Action("call", 0, 0, 0);
        client.sendTCP(resp);
        //System.out.println("client.isConnected() = " + client.isConnected());
        //System.out.println("client.getKryo() = " + client.getKryo());
    }

    public void sendRaise(int playerId, float amount) {
        //System.out.println("Отправляю действие: " + "Raise" + " от игрока: " + playerId);
        ActionResponse resp = new ActionResponse();
        resp.playerId = playerId;
        resp.chosenAction = new Action("raise", amount, 0, 0);
        resp.amount = amount;
        client.sendTCP(resp);
    }

    public void sendGameStart() {
        client.sendTCP(new GameStartRequest());
    }
    public void sendReadyForNextRound(int playerId, boolean isReady) {
        ClientReadyForNextRound packet = new ClientReadyForNextRound(playerId, isReady);
        client.sendTCP(packet);
    }

    public void setHost(boolean host) {
        this.host = host;
    }

    public boolean isHost() {
        return host;
    }

    public String getNickName() {
        return name;
    }

    public void sendChatMessage(String text) {
        if (name == null) {
            System.err.println("⚠️ Невозможно отправить сообщение: имя игрока не задано!");
            return;
        }
        client.sendTCP(new ChatMessage(text, name));
    }

    public int getMyId() {
        return clientId;
    }

    public void sendRestart() {
        client.sendTCP(new RestartGameRequest());
    }
    public void onWinnerPacket(WinnerPacket packet) {
        String winnerName = packet.getName();

        Gdx.app.postRunnable(() -> {
            if (winnerName.equals(this.name)) {
                pokerApp.setScreen(new WinnerScreen(this.pokerApp, winnerName));
            } else {
                pokerApp.setScreen(new LobbyScreen(this.pokerApp));
            }
        });
    }

}
