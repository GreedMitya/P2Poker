package GreedMitya.P2Poker.PacketsClasses;

public class WinnerPacket {
    String name;

    public WinnerPacket(){}

    public void setName(String name) {
        this.name = name;
    }

    public String getName(){
        return name;
    }
}
