package GreedMitya.P2Poker;

import GreedMitya.P2Poker.PacketsClasses.ChatMessage;
import GreedMitya.P2Poker.PacketsClasses.GameStartedNotification;
import GreedMitya.P2Poker.PacketsClasses.PlayerJoinedNotification;


public interface ChatListener {
    void onChatMessage(ChatMessage msg);
    void onGameStarted(GameStartedNotification note);
    void onPlayerJoinedNotification(PlayerJoinedNotification object);
}
