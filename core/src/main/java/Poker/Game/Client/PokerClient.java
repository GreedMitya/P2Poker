package Poker.Game.Client;

import Poker.Game.ClientListener;
import Poker.Game.PacketsClasses.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Клиент для покер-рума, «чистый» от консоли, отдаёт все события в ClientListener.
 */
public class PokerClient {
    private int clientId;
    private Client client;
    private ClientListener listener;
    private boolean host;
    private String name;
    private final Map<Integer, String> idToNickname   = new HashMap<>();
    private final Map<String, Integer> nicknameToId   = new HashMap<>();


    public void registerPlayer(int id, String nickname) {
        idToNickname.put(id, nickname);
        nicknameToId.put(nickname, id);
        // если это мы — сохраняем clientId
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


    /** Зарегистрировать слушателя событий (UI, логика и т.п.). */
    public void setListener(ClientListener listener) {
        this.listener = listener;
    }

    /**
     * Запустить клиент: установить соединение и отправить JoinRequest.
     *
     * @param hostIP   IP-адрес сервера
     * @param nickname Никнейм игрока
     * @throws IOException если не удалось подключиться
     */
    public void start(String hostIP, String nickname) throws IOException {
        client = new Client();
        client.start();

        // Регистрация всех пакетов
        Kryo kryo = client.getKryo();
        Network.register(kryo);

        // Во всех потоках KryoNet — ловим uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("💥 Uncaught exception in thread " + thread.getName());
            throwable.printStackTrace();
        });

        // Листенер для входящих пакетов
        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (listener == null) return;

                if (object instanceof JoinResponse) {
                    listener.onJoinResponse((JoinResponse) object);

                }else if (object instanceof PlayerListUpdate) {
                    PlayerListUpdate update = (PlayerListUpdate) object;

                    // Регистрируем всех игроков: nickname → id
                    for (Map.Entry<String, Integer> entry : update.getNicknames().entrySet()) {
                        String nickname = entry.getKey();
                        int playerId = entry.getValue();
                        registerPlayer(playerId, nickname);
                    }
                    listener.onPlayerListUpdate(update.getNicknamesOnly());
                }
                else if (object instanceof GameStartedNotification) {
                    listener.onGameStarted((GameStartedNotification) object);
                } else if (object instanceof CardInfo) {
                    listener.onCardInfo((CardInfo) object);
                } else if (object instanceof GameStats) {
                    listener.onGameStats((GameStats) object);
                } else if (object instanceof BlindsNotification) {
                    listener.onBlinds((BlindsNotification) object);
                } else if (object instanceof ActionRequest) {
                    listener.onActionRequest((ActionRequest) object);
                } else if (object instanceof PlayerBalanceUpdate) {
                    listener.onPlayerBalanceUpdate((PlayerBalanceUpdate) object);
                } else if (object instanceof TableCardsInfo) {
                    listener.onTableCardsInfo((TableCardsInfo) object);
                } else if (object instanceof PlayerBetUpdate) {
                    listener.onPlayerBetUpdate((PlayerBetUpdate) object);
                } else if (object instanceof ChatMessage) {
                    ChatMessage message = (ChatMessage) object;
                    listener.onChatMessage(message);
                } else if (object instanceof  PotUpdate) {
                    listener.onPotUpdate((PotUpdate) object);
                }else if (object instanceof FoldNotification) {
                    listener.onPlayerFold((FoldNotification) object);
                }else if(object instanceof EndOfHandPacket){
                    listener.onEndOfHandPacket((EndOfHandPacket) object);
                }
            }

            @Override
            public void disconnected(Connection connection) {
                if (listener != null) {
                    listener.onDisconnected();
                }
            }
        });

        // Подключаемся к серверу (таймаут 5000 ms, TCP 54555, UDP 54777)
        client.connect(5000, hostIP, 54555, 54777);

        // Отправляем запрос на присоединение
        this.name = nickname;
        client.sendTCP(new JoinRequest(nickname));

    }

    // === Методы для UI / контроллера, вызываемые при клике на кнопки ===

    public void sendCheck(int playerId) {
        ActionResponse resp = new ActionResponse();
        resp.playerId = playerId;
        resp.chosenAction = new Action("check", 0, 0, 0);
        client.sendTCP(resp);
    }

    public void sendFold(int playerId) {
        ActionResponse resp = new ActionResponse();
        resp.playerId = playerId;
        resp.chosenAction = new Action("fold", 0, 0, 0);
        client.sendTCP(resp);
    }

    public void sendCall(int playerId) {
        ActionResponse resp = new ActionResponse();
        resp.playerId = playerId;
        resp.chosenAction = new Action("call", 0, 0, 0);
        client.sendTCP(resp);
    }

    public void sendRaise(int playerId, float amount) {
        ActionResponse resp = new ActionResponse();
        resp.playerId = playerId;
        resp.chosenAction = new Action("raise", amount, 0, 0);
        resp.amount = amount;
        client.sendTCP(resp);
    }

    /** Если нужно дать хосту UI-кнопку «Start Game». */
    public void sendGameStart() {
        client.sendTCP(new GameStartRequest());
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
}
