package GreedMitya.P2Poker;


import GreedMitya.P2Poker.PacketsClasses.*;


import java.util.List;

public interface ClientListener {
    void onPlayerListUpdate(List<String> nicknames);

    void onCheckPacket();

    void onCardInfo(CardInfo info);
    void onBlinds(BlindsNotification note);
    void onActionRequest(ActionRequest req);
    void onDisconnected();
    void onPlayerBalanceUpdate(PlayerBalanceUpdate update);
    void onTableCardsInfo(TableCardsInfo tableCardsInfo);
    void onPlayerBetUpdate(PlayerBetUpdate update);
    void onPotUpdate(PotUpdate potUpdate);
    void onPlayerFold(FoldNotification notif);
    void onEndOfHandPacket(EndOfHandPacket packet);
    void onGameRestart();
    void onPlayerOrderPacket(PlayerOrderPacket object);
    void onBetUpdatePack(BetUpdatePack object);
}
