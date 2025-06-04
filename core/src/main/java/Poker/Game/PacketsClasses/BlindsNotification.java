package Poker.Game.PacketsClasses;

import java.util.List;

public class BlindsNotification {
    private List<String> messages;
    private int dealerId;

    public BlindsNotification() {}

    public BlindsNotification(List<String> messages, int dealerId) {
        this.messages = messages;
        this.dealerId = dealerId;
    }

    public List<String> getMessages() {
        return messages;
    }

    public int getDealerId() {
        return dealerId;
    }
}

