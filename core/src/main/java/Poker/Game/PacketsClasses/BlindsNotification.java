package Poker.Game.PacketsClasses;

import Poker.Game.core.Player;

import java.util.ArrayList;

public class BlindsNotification {
   String SmallBlind;

    public BlindsNotification(){
    }
    public BlindsNotification(String smallBlind){
        SmallBlind = smallBlind;
    }
    public String getSmallBlind(){
        return SmallBlind;
    }
}
