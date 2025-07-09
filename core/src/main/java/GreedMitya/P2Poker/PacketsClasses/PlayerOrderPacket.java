package GreedMitya.P2Poker.PacketsClasses;

import java.util.List;

public class PlayerOrderPacket {
    public List<String> logicalOrder;

    public PlayerOrderPacket(List<String> logicalOrder){
        this.logicalOrder = logicalOrder;
    }
    public PlayerOrderPacket(){}

    public List<String> getLogicalOrder() {
        return logicalOrder;
    }
}

