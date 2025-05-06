package Poker.Game.PacketsClasses;


public class JoinResponse {
    public boolean success;
    public String message;

    public JoinResponse() {}

    public JoinResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}

