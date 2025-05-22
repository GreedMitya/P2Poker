package Poker.Game;

import Poker.Game.PacketsClasses.ChatMessage;
import Poker.Game.PacketsClasses.GameStartedNotification;
import Poker.Game.PacketsClasses.GameStats;
import Poker.Game.PacketsClasses.PlayerJoinedNotification;
import com.badlogic.gdx.Gdx;

public interface ChatListener {
    void onChatMessage(ChatMessage msg);
    void onGameStarted(GameStartedNotification note);
    void onGameStats(GameStats object);
    void onPlayerJoinedNotification(PlayerJoinedNotification object);
}
