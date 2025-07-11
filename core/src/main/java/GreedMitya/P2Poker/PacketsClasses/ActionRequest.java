package GreedMitya.P2Poker.PacketsClasses;


public class ActionRequest {
    public int playerId;
    public Action[] availableActions;
    public int timeoutSec;
    public ActionRequest() {}
}
