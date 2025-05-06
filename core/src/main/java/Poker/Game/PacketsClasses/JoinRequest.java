package Poker.Game.PacketsClasses;

public class JoinRequest {
    public String nickname;

    public JoinRequest() {} // Kryo нужен пустой конструктор

    public JoinRequest(String nickname) {
        this.nickname = nickname;
    }
}


