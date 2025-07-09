package GreedMitya.P2Poker.PacketsClasses;

import java.util.List;
import java.util.Map;

public class PlayerListUpdate {
    public Map<String,Integer> nicknames;

    public PlayerListUpdate(Map<String,Integer> nicknames) {
        this.nicknames = (Map<String, Integer>) nicknames;
    }
    public PlayerListUpdate(){

    }

    public Map<String, Integer> getNicknames(){
        return nicknames;
    }

    public List<String> getNicknamesOnly() {
        return new java.util.ArrayList<>(nicknames.keySet());
        }
}

