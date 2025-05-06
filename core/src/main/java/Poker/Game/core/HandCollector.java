package Poker.Game.core;


import java.util.ArrayList;
import java.util.List;

public class HandCollector {
    private List<Card> totalHand;
    private static Player player;
    private static Table table;

    public HandCollector(Player player, Table table){
        this.player = player;
        this.table = table;
    }
    public HandCollector(){

    }
    public static List<Card> getTotalHand(){
        List<Card> totalHand = new ArrayList<Card>();
        totalHand.addAll(player.getHand());
        totalHand.addAll(table.board);
        return totalHand;
    }



}
