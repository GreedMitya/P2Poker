package GreedMitya.P2Poker.PacketsClasses;

public class PlayerJoinedNotification {
    public String nickname;
    public int ID;

    public PlayerJoinedNotification() {}

    public PlayerJoinedNotification(String nickname,int ID) {
        this.nickname = nickname;
        this.ID = ID;
    }
}

