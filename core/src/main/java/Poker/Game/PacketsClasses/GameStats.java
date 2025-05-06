package Poker.Game.PacketsClasses;

public class GameStats {
    String stats;


    public GameStats(){
    }
    public GameStats(int a, int b){
        stats ="\n" + "Hand number: " + a + "\n" + "Players: " + b;
    }

    public String getStats(){
        return stats;
    }
}
