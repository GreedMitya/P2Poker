package Poker.Game.PacketsClasses;

import Poker.Game.core.*;
import com.esotericsoftware.kryo.Kryo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Network {
    public static void register(Kryo kryo) {
        kryo.register(Integer.class);
        kryo.register(String[].class);
        kryo.register(String.class);
        kryo.register(ArrayList.class);
        kryo.register(List.class);
        kryo.register(Map.class);
        kryo.register(HashMap.class);
        kryo.register(ConcurrentHashMap.class);
        kryo.register(SpectatorJoinedNotification.class);
        kryo.register(EndOfHandPacket.class);
        kryo.register(PlayerOrderPacket.class);
        kryo.register(FoldNotification.class);
        kryo.register(PotUpdate.class);
        kryo.register(PlayerBetUpdate.class);
        kryo.register(BetUpdatePack.class);
        kryo.register(ReturnToLobbyPacket.class);
        kryo.register(WinnerPacket.class);
        kryo.register(ClearBetsNotification.class);
        kryo.register(TableCardsInfo.class);
        kryo.register(ChatMessage.class);
        kryo.register(RestartGameRequest.class);
        kryo.register(RestartGameNotification.class);
        kryo.register(ClientReadyForNextRound.class);
        kryo.register(PlayerAction.class);
        kryo.register(LinkedList.class);
        kryo.register(Logger.class);
        kryo.register(JoinRequest.class);
        kryo.register(CheckPacket.class);
        kryo.register(PlayerJoinedNotification.class);
        kryo.register(PlayerListUpdate.class);
        kryo.register(Poker.Game.PacketsClasses.Action[].class);
        kryo.register(Poker.Game.PacketsClasses.Action.class);
        kryo.register(Poker.Game.PacketsClasses.ActionRequest.class);
        kryo.register(Poker.Game.PacketsClasses.ActionResponse.class);
        kryo.register(KeepAlive.class);
        kryo.register(Card.Rank.class);
        kryo.register(Card.Suit.class);
        kryo.register(GameStartRequest.class);
        kryo.register(GameStartedNotification.class);
        kryo.register(BlindsNotification.class);
        kryo.register(StartGame.class);
        kryo.register(PokerGame.class);
        kryo.register(RoundCounter.class);
        kryo.register(Player.class);
        kryo.register(PlayerState.class);
        kryo.register(PlayerManager.class);
        kryo.register(BettingManager.class);
        kryo.register(Card.class);
        kryo.register(CardInfo.class);
        kryo.register(Deck.class);
        kryo.register(Table.class);
        kryo.register(HandCollector.class);
        kryo.register(HandEvaluator.class);
        kryo.register(PotManager.class);
        kryo.register(SidePot.class);
        kryo.register(HandEvaluatorTest.class);
        kryo.register(PlayerBalanceUpdate.class);
    }
}
