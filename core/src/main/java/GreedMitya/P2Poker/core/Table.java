package GreedMitya.P2Poker.core;

import GreedMitya.P2Poker.PacketsClasses.Logger;
import GreedMitya.P2Poker.PacketsClasses.TableCardsInfo;
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
        Logger.Game("Dealing Flop...");
        board.add(deck.dealCard());
        board.add(deck.dealCard());
        board.add(deck.dealCard());
        TableCardsInfo tableCardsInfo = new TableCardsInfo(board);
        server.sendToAllTCP(tableCardsInfo);
    }

    public void dealTurn(Deck deck) {
        Logger.Game("Dealing Turn...");
        board.add(deck.dealCard());
        TableCardsInfo tableCardsInfo = new TableCardsInfo(board);
        server.sendToAllTCP(tableCardsInfo);
    }

    public void dealRiver(Deck deck) {
        Logger.Game("Dealing River...");
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
        Logger.Game(board+"");
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
