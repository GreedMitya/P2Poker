package GreedMitya.P2Poker.PacketsClasses;

import GreedMitya.P2Poker.core.*;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Network {
    public static void register(Kryo kryo) {
        // Стандартные классы
        kryo.register(Integer.class);
        kryo.register(String.class);
        kryo.register(String[].class);
        kryo.register(ArrayList.class);
        kryo.register(LinkedList.class);
        kryo.register(List.class);
        kryo.register(Map.class);
        kryo.register(HashMap.class);
        kryo.register(ConcurrentHashMap.class);
        kryo.register(Logger.class);

        // Enum-классы
        kryo.register(Card.Rank.class);
        kryo.register(Card.Suit.class);
        kryo.register(PlayerState.class);

        // Классы core и игровая логика
        kryo.register(Card.class);
        kryo.register(Card.Suit.class, new DefaultSerializers.EnumSerializer(Card.Suit.class));
        kryo.register(Card.Rank.class, new DefaultSerializers.EnumSerializer(Card.Rank.class));
        kryo.register(CardInfo.class);
        kryo.register(Deck.class);
        kryo.register(Player.class);
        kryo.register(PlayerManager.class);
        kryo.register(BettingManager.class);
        kryo.register(PokerGame.class);
        kryo.register(HandCollector.class);
        kryo.register(HandEvaluator.class);
        kryo.register(HandEvaluatorTest.class);
        kryo.register(PotManager.class);
        kryo.register(SidePot.class);
        kryo.register(RoundCounter.class);
        kryo.register(Table.class);

        // Пакеты и сетевые сообщения
        kryo.register(Action.class);
        kryo.register(Action[].class); // важно после Action
        kryo.register(ActionRequest.class);
        kryo.register(ActionResponse.class);
        kryo.register(BetUpdatePack.class);
        kryo.register(BlindsNotification.class);
        kryo.register(CheckPacket.class);
        kryo.register(ChatMessage.class);
        kryo.register(ClearBetsNotification.class);
        kryo.register(ClientReadyForNextRound.class);
        kryo.register(EndOfHandPacket.class);
        kryo.register(FoldNotification.class);
        kryo.register(GameStartRequest.class);
        kryo.register(GameStartedNotification.class);
        kryo.register(JoinRequest.class);
        kryo.register(KeepAlive.class);
        kryo.register(PlayerAction.class);
        kryo.register(PlayerBalanceUpdate.class);
        kryo.register(PlayerBetUpdate.class);
        kryo.register(PlayerJoinedNotification.class);
        kryo.register(PlayerListUpdate.class);
        kryo.register(PlayerOrderPacket.class);
        kryo.register(PotUpdate.class);
        kryo.register(RestartGameNotification.class);
        kryo.register(RestartGameRequest.class);
        kryo.register(ReturnToLobbyPacket.class);
        kryo.register(SpectatorJoinedNotification.class);
        kryo.register(StartGame.class);
        kryo.register(TableCardsInfo.class);
        kryo.register(WinnerPacket.class);
    }
}
