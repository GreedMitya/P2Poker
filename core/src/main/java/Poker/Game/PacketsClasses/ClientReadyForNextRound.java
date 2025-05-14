package Poker.Game.PacketsClasses;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.FieldSerializer;

public class ClientReadyForNextRound {
    private int playerId;
    private boolean isReady;

    public ClientReadyForNextRound() {
        // Конструктор по умолчанию для Kryo
    }

    public ClientReadyForNextRound(int playerId, boolean isReady) {
        this.playerId = playerId;
        this.isReady = isReady;
    }

    public int getPlayerId() {
        return playerId;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public static void register(Kryo kryo) {
        kryo.register(ClientReadyForNextRound.class, new FieldSerializer<>(kryo, ClientReadyForNextRound.class));
    }
}

