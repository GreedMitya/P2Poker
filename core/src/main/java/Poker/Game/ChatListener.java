package Poker.Game;

import Poker.Game.PacketsClasses.ChatMessage;
import Poker.Game.PacketsClasses.GameStartedNotification;
import Poker.Game.PacketsClasses.PlayerJoinedNotification;


public interface ChatListener {
    void onChatMessage(ChatMessage msg);
    void onGameStarted(GameStartedNotification note);
    void onPlayerJoinedNotification(PlayerJoinedNotification object);
}
