package GreedMitya.P2Poker.PacketsClasses;


public class ChatMessage {
    public String message;
    public String name;

    public ChatMessage() {
        // KryoNet требует пустой конструктор
    }

    public ChatMessage(String message, String name) {
        this.message = message;
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public String getName() {
        return name;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setName(String name) {
        this.name = name;
    }
}

