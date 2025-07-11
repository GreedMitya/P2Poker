package GreedMitya.P2Poker.core;

import java.util.*;

public class HandEvaluatorTest {

    public static void main(String[] args) throws InterruptedException {
        for (Card.Rank rank : Card.Rank.values()) {
            System.out.println(rank + " -> " + rank.ordinal());
        }

        testRandomHands(1000000);
        Thread.sleep(1000);
        // testKnownHands();
    }

    private synchronized static void testKnownHands() {
        System.out.println("Тест известных комбинаций...");
        Deck deck1 = new Deck();
        Table table = new Table(deck1);
        Player player1 = new Player("Игрок 1");
        Player player2 = new Player("Игрок 2");
        List<Player> players = new ArrayList<>();
        players.add(player1);
        players.add(player2);
        PlayerManager playerManager = new PlayerManager((ArrayList<Player>) players);

        player1.getHand().addAll(Arrays.asList(
            new Card(Card.Rank.King, Card.Suit.Spades),
            new Card(Card.Rank.King, Card.Suit.Diamonds)
        ));
        player2.getHand().addAll(Arrays.asList(
            new Card(Card.Rank.Ten, Card.Suit.Diamonds),
            new Card(Card.Rank.Ace, Card.Suit.Diamonds)
        ));
        table.board.addAll(Arrays.asList(
            new Card(Card.Rank.Ace, Card.Suit.Hearts),
            new Card(Card.Rank.Queen, Card.Suit.Clubs),
            new Card(Card.Rank.Queen, Card.Suit.Hearts),
            new Card(Card.Rank.Two, Card.Suit.Hearts),
            new Card(Card.Rank.Two, Card.Suit.Clubs)
        ));

        for (Player p : playerManager.getPlayers()) {
            HandCollector handCollector = new HandCollector(p, table);
            p.setTotalHand();
            p.evaluateHand();
            p.setCombination();
            System.out.println(p.getName() + ": " + p.getTotalHand() + "; Value: " + p.getHandValue() + ";" + p.getCombination() + "; " + p.getNameofCombination() + ";");
        }
        List<String> winner = new ArrayList<>();
        if (player1.getHandValue() > player2.getHandValue()) {
            winner.add(player1.getName());
        } else if (player1.getHandValue() == player2.getHandValue()) {
            winner.add(player1.getName());
            winner.add(player2.getName());
        } else {
            winner.add(player2.getName());
        }
        System.out.println("Winner is: " + winner + ";");
    }

    private synchronized static void testRandomHands(int numberOfTests) {
        System.out.println("\nГенерация случайных рук...");
        Map<String, Integer> handCounts = new LinkedHashMap<>();
        List<String> handOrder = Arrays.asList(
            "Royal flush!", "Straight flush!", "Four of a kind!",
            "Full house!", "Flush!", "Straight!", "Three of a kind!", "Two pairs!",
            "One pair!", "High cards!"
        );
        for (String hand : handOrder) {
            handCounts.put(hand, 0);
        }

        for (int i = 0; i < numberOfTests; i++) {
            Deck deck = new Deck();
            Table table = new Table(deck);
            deck.shuffle();
            Player player1 = new Player("Игрок 1");
            Player player2 = new Player("Игрок 2");
            List<Player> players = new ArrayList<>();
            players.add(player1);
            players.add(player2);
            PlayerManager playerManager = new PlayerManager((ArrayList<Player>) players);

            player1.getHand().addAll(Arrays.asList(
                deck.dealCard(),
                deck.dealCard()
            ));
            player2.getHand().addAll(Arrays.asList(
                deck.dealCard(),
                deck.dealCard()
            ));
            table.board.add(deck.dealCard());
            table.board.add(deck.dealCard());
            table.board.add(deck.dealCard());
            table.board.add(deck.dealCard());
            table.board.add(deck.dealCard());
            System.out.println("----------------------------------------");

            List<Player> activePlayers = new ArrayList<>();
            for (Player player : playerManager.getActivePlayers()) {
                if (!player.isFolded()) {
                    activePlayers.add(player);
                }
            }
            for (Player p : activePlayers) {
                HandCollector handCollector = new HandCollector(p, table);
                p.setTotalHand();
                p.evaluateHand();
                p.setCombination();
                String handType = p.getNameofCombination();
                handCounts.put(handType, handCounts.getOrDefault(handType, 0) + 1);
                System.out.println(p.getName() + ": " + p.getTotalHand() + "; Value: " + p.getHandValue() + ";" + p.getCombination() + "; " + p.getNameofCombination() + ";");
            }
            List<String> winner = new ArrayList<>();
            if (player1.getHandValue() > player2.getHandValue()) {
                winner.add(player1.getName());
            } else if (player1.getHandValue() == player2.getHandValue()) {
                winner.add(player1.getName());
                winner.add(player2.getName());
            } else {
                winner.add(player2.getName());
            }
            System.out.println("Winner is: " + winner + ";");
        }
        System.out.println("-------------------------------------");
        System.out.println("Тест на " + numberOfTests * 2 + " рук: ");
        for (String hand : handOrder) {
            int count = handCounts.get(hand);
            double percentage = (count * 100) / (double) (numberOfTests * 2);
            System.out.printf("%-18s: %6d (%.4f%%)%n", hand, count, percentage);
        }
    }
}

