package Poker.Game.core;

import Poker.Game.PacketsClasses.TableCardsInfo;
import com.esotericsoftware.kryonet.Server;

import java.util.ArrayList;

public class Table {
    private Server server;
    public ArrayList<Card> board;



    public Table(Deck deck) {
        this.board = new ArrayList<>();
    }
    public Table(){

    }

    public void dealFlop(Deck deck) {
        System.out.println("Dealing Flop...");
        board.add(deck.dealCard());
        board.add(deck.dealCard());
        board.add(deck.dealCard());
        TableCardsInfo tableCardsInfo = new TableCardsInfo(board);
        server.sendToAllTCP(tableCardsInfo);
    }

    public void dealTurn(Deck deck) {
        System.out.println("Dealing Turn...");
        board.add(deck.dealCard());
        TableCardsInfo tableCardsInfo = new TableCardsInfo(board);
        server.sendToAllTCP(tableCardsInfo);
    }

    public void dealRiver(Deck deck) {
        System.out.println("Dealing River...");
        board.add(deck.dealCard());
        TableCardsInfo tableCardsInfo = new TableCardsInfo(board);
        server.sendToAllTCP(tableCardsInfo);
    }

    public void clearBoard(Deck deck) {
        board.forEach(deck::returnCard);
        board.clear();
        TableCardsInfo tableCardsInfo = new TableCardsInfo(board);
        server.sendToAllTCP(tableCardsInfo);
    }
    public void showBoard(){
        System.out.println(board);
    }
    public ArrayList<Card> getBoard(){
        return board;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Server getServer() {
        return server;
    }
}
