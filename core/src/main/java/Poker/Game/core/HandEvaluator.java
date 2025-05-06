package Poker.Game.core;


import java.util.*;

public class HandEvaluator {

    public HandEvaluator(){

    }
    public static synchronized int evaluateHand(List<Card> totalHand) {
        return evaluateCardHand(totalHand);
    }

    public static synchronized List<Card> getCombination(List<Card> totalHand) {
        return evaluateCombination(totalHand);
    }

    private synchronized static List<Card> evaluateCombination(List<Card> totalHand) {
        if (isRoyalFlush(totalHand)) return getRoyalFlushCard(totalHand);
        if (isStraightFlush(totalHand)) return getStraightCards(totalHand);
        if (isFourOfAKind(totalHand)) return getFourofAKindCards(totalHand);
        if (isFullHouse(totalHand)) return getFullHouseCards(totalHand);
        if (isFlush(totalHand)) return getFlushCards(totalHand);
        if (isStraight(totalHand)) return getStraightCards(totalHand);
        if (isThreeOfAKind(totalHand)) return getThreeOfAKindCards(totalHand);
        if (isTwoPair(totalHand)) return getTwoPairsCards(totalHand);
        if (isOnePair(totalHand)) return getOnePairCards(totalHand);
        return getHighCards(totalHand);
    }

    private synchronized static int evaluateCardHand(List<Card> totalHand) {
        if (isRoyalFlush(totalHand)) return 100000000;
        if (isStraightFlush(totalHand)) return 90000000 + getStraightValue(getStraightCards(totalHand));
        if (isFourOfAKind(totalHand)) return 80000000 + getValueFourOfAKind(totalHand);
        if (isFullHouse(totalHand)) return 70000000 + getValueFullHouse(totalHand);
        if (isFlush(totalHand)) return 60000000 + getHighCardValue(getFlushCards(totalHand));
        if (isStraight(totalHand)) return 50000000 + getStraightValue(getStraightCards(totalHand));
        if (isThreeOfAKind(totalHand)) return 40000000 + getValueThreeOfAKind(totalHand);
        if (isTwoPair(totalHand)) return 30000000 + getTwoPairValue(totalHand);
        if (isOnePair(totalHand)) return 20000000 + getOnePairValue(totalHand);
        return 10000000 + getHighCardsValue(totalHand);
    }

    private synchronized static int getHighCardValue(List<Card> hand) {
        int max = 0;
        for (Card card : hand) {
            int rank = card.getRank().ordinal();
            if (rank > max) max = rank;
        }
        return max;
    }

    private synchronized static boolean countSameRank(List<Card> totalHand, int count) {
        Map<Card.Rank, Integer> rankCount = new HashMap<>();
        for (Card card : totalHand) {
            rankCount.put(card.getRank(), rankCount.getOrDefault(card.getRank(), 0) + 1);
        }
        for (Integer value : rankCount.values()) {
            if (value == count) return true;
        }
        return false;
    }

    private synchronized static long countPairs(List<Card> totalHand) {
        Map<Card.Rank, Integer> rankCount = new HashMap<>();
        for (Card card : totalHand) {
            rankCount.put(card.getRank(), rankCount.getOrDefault(card.getRank(), 0) + 1);
        }
        long pairCount = 0;
        for (Integer value : rankCount.values()) {
            if (value == 2) pairCount++;
        }
        return pairCount;
    }

    private synchronized static boolean isRoyalFlush(List<Card> totalHand) {
        if (!isStraightFlush(totalHand)) return false;
        Set<Card.Rank> ranks = new HashSet<>();
        for (Card card : totalHand) {
            ranks.add(card.getRank());
        }
        return ranks.containsAll(Arrays.asList(Card.Rank.Ten, Card.Rank.Jack, Card.Rank.Queen, Card.Rank.King, Card.Rank.Ace));
    }

    private synchronized static List<Card> getRoyalFlushCard(List<Card> totalHand) {
        Map<Card.Suit, List<Card>> suitGroups = new HashMap<>();
        for (Card card : totalHand) {
            suitGroups.computeIfAbsent(card.getSuit(), k -> new ArrayList<>()).add(card);
        }
        for (List<Card> suitedCards : suitGroups.values()) {
            if (suitedCards.size() >= 5) {
                Collections.sort(suitedCards, Collections.reverseOrder());
                return suitedCards.subList(0, 5);
            }
        }
        return Collections.emptyList();
    }

    private synchronized static boolean isStraightFlush(List<Card> totalHand) {
        Map<Card.Suit, List<Card>> groupSuits = new HashMap<>();
        for (Card card : totalHand) {
            groupSuits.computeIfAbsent(card.getSuit(), k -> new ArrayList<>()).add(card);
        }
        for (List<Card> suitedCards : groupSuits.values()) {
            if (suitedCards.size() >= 5) {
                Collections.sort(suitedCards, new Comparator<Card>() {
                    @Override
                    public int compare(Card c1, Card c2) {
                        return Integer.compare(c1.getRank().ordinal(), c2.getRank().ordinal());
                    }
                });
                if (isStraight(suitedCards)) return true;
            }
        }
        return false;
    }

    private synchronized static boolean isFourOfAKind(List<Card> totalHand) {
        return countSameRank(totalHand, 4);
    }

    private synchronized static List<Card> getFourofAKindCards(List<Card> totalHand) {
        Map<Card.Rank, Integer> rankCount = new HashMap<>();
        for (Card card : totalHand) {
            rankCount.put(card.getRank(), rankCount.getOrDefault(card.getRank(), 0) + 1);
        }
        int fourOfSameRank = -1;
        for (Map.Entry<Card.Rank, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() == 4) {
                fourOfSameRank = entry.getKey().ordinal();
                break;
            }
        }
        if (fourOfSameRank == -1) return Collections.emptyList();

        List<Card> hand = new ArrayList<>();
        for (Card card : totalHand) {
            if (card.getRank().ordinal() == fourOfSameRank) {
                hand.add(card);
            }
        }
        List<Card> remainingCards = new ArrayList<>();
        for (Card card : totalHand) {
            if (card.getRank().ordinal() != fourOfSameRank) {
                remainingCards.add(card);
            }
        }
        List<Card> highCards = getHighCards(remainingCards);
        hand.add(highCards.get(0));
        return hand;
    }

    private synchronized static int getValueFourOfAKind(List<Card> totalHand) {
        if (!isFourOfAKind(totalHand)) return 0;
        Map<Card.Rank, Integer> rankCount = new HashMap<>();
        for (Card card : totalHand) {
            rankCount.put(card.getRank(), rankCount.getOrDefault(card.getRank(), 0) + 1);
        }
        int four = 0;
        for (Map.Entry<Card.Rank, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() == 4) {
                four = entry.getKey().ordinal();
                break;
            }
        }
        List<Integer> kickers = new ArrayList<>();
        for (Card card : totalHand) {
            if (card.getRank().ordinal() != four) {
                kickers.add(card.getRank().ordinal());
            }
        }
        Collections.sort(kickers, Collections.reverseOrder());
        return (four * 10000) + (kickers.get(0) * 100);
    }

    private synchronized static boolean isFullHouse(List<Card> totalHand) {
        Map<Card.Rank, Integer> rankCounts = new HashMap<>();
        for (Card card : totalHand) {
            rankCounts.put(card.getRank(), rankCounts.getOrDefault(card.getRank(), 0) + 1);
        }
        boolean hasThree = false;
        boolean hasPair = false;
        for (Integer count : rankCounts.values()) {
            if (count == 3) {
                if (hasThree) hasPair = true;
                else hasThree = true;
            } else if (count == 2) {
                hasPair = true;
            }
        }
        return hasThree && hasPair;
    }

    private synchronized static List<Card> getFullHouseCards(List<Card> totalHand) {
        Map<Card.Rank, Integer> rankCounts = new HashMap<>();
        for (Card card : totalHand) {
            rankCounts.put(card.getRank(), rankCounts.getOrDefault(card.getRank(), 0) + 1);
        }
        List<Integer> threes = new ArrayList<>();
        List<Integer> pairs = new ArrayList<>();
        for (Map.Entry<Card.Rank, Integer> entry : rankCounts.entrySet()) {
            if (entry.getValue() == 3) threes.add(entry.getKey().ordinal());
            if (entry.getValue() >= 2) pairs.add(entry.getKey().ordinal());
        }
        Collections.sort(threes, Collections.reverseOrder());
        int threeOfKind = threes.get(0);
        for (int pair : pairs) {
            if (pair != threeOfKind) {
                pairs = new ArrayList<>();
                pairs.add(pair);
                break;
            }
        }
        int second = pairs.get(0);

        List<Card> hand = new ArrayList<>();
        for (Card card : totalHand) {
            if (card.getRank().ordinal() == threeOfKind && hand.size() < 3) {
                hand.add(card);
            }
        }
        for (Card card : totalHand) {
            if (card.getRank().ordinal() == second && hand.size() < 5) {
                hand.add(card);
            }
        }
        return hand;
    }

    private synchronized static int getValueFullHouse(List<Card> totalHand) {
        if (!isFullHouse(totalHand)) return 0;
        Map<Card.Rank, Integer> rankCounts = new HashMap<>();
        for (Card card : totalHand) {
            rankCounts.put(card.getRank(), rankCounts.getOrDefault(card.getRank(), 0) + 1);
        }
        List<Integer> threes = new ArrayList<>();
        for (Map.Entry<Card.Rank, Integer> entry : rankCounts.entrySet()) {
            if (entry.getValue() == 3) threes.add(entry.getKey().ordinal());
        }
        Collections.sort(threes, Collections.reverseOrder());
        int bestThree = threes.get(0);

        List<Integer> pairs = new ArrayList<>();
        for (Map.Entry<Card.Rank, Integer> entry : rankCounts.entrySet()) {
            if (entry.getValue() >= 2 && entry.getKey().ordinal() != bestThree) {
                pairs.add(entry.getKey().ordinal());
            }
        }
        Collections.sort(pairs, Collections.reverseOrder());
        int bestPair = pairs.get(0);

        return (bestThree * 10000) + (bestPair * 100);
    }

    private synchronized static boolean isFlush(List<Card> totalHand) {
        Map<Card.Suit, Integer> suitCount = new HashMap<>();
        for (Card card : totalHand) {
            suitCount.put(card.getSuit(), suitCount.getOrDefault(card.getSuit(), 0) + 1);
        }
        for (Integer count : suitCount.values()) {
            if (count >= 5) return true;
        }
        return false;
    }

    private synchronized static List<Card> getFlushCards(List<Card> totalHand) {
        Map<Card.Suit, List<Card>> suitGroups = new HashMap<>();
        for (Card card : totalHand) {
            suitGroups.computeIfAbsent(card.getSuit(), k -> new ArrayList<>()).add(card);
        }
        for (List<Card> suitedCards : suitGroups.values()) {
            if (suitedCards.size() >= 5) {
                Collections.sort(suitedCards, new Comparator<Card>() {
                    @Override
                    public int compare(Card c1, Card c2) {
                        return c2.getRank().ordinal() - c1.getRank().ordinal();
                    }
                });
                return suitedCards.subList(0, 5);
            }
        }
        return Collections.emptyList();
    }

    private synchronized static boolean isStraight(List<Card> totalHand) {
        Set<Integer> cardRanks = new TreeSet<>();
        for (Card card : totalHand) {
            cardRanks.add(card.getRank().ordinal());
        }
        if (cardRanks.size() < 5) return false;

        List<Integer> ranksList = new ArrayList<>(cardRanks);
        Collections.sort(ranksList);
        int count = 1;
        for (int i = 1; i < ranksList.size(); i++) {
            if (ranksList.get(i) == ranksList.get(i - 1) + 1) {
                count++;
                if (count == 5) return true;
            } else {
                count = 1;
            }
        }
        return cardRanks.containsAll(Arrays.asList(0, 1, 2, 3, 12));
    }

    public synchronized static int getStraightValue(List<Card> hand) {
        Set<Integer> cardRanks = new TreeSet<>();
        for (Card card : hand) {
            cardRanks.add(card.getRank().ordinal());
        }
        if (cardRanks.size() < 5) return 0;

        List<Integer> ranksList = new ArrayList<>(cardRanks);
        Collections.sort(ranksList);
        int maxRank = 0;
        int count = 1;
        for (int i = 1; i < ranksList.size(); i++) {
            if (ranksList.get(i) == ranksList.get(i - 1) + 1) {
                count++;
                maxRank = ranksList.get(i);
                if (count == 5) return maxRank;
            } else {
                count = 1;
            }
        }
        if (cardRanks.containsAll(Arrays.asList(0, 1, 2, 3, 12))) {
            return 3;
        }
        return 0;
    }

    public synchronized static List<Card> getStraightCards(List<Card> totalHand) {
        TreeSet<Integer> uniqueRanks = new TreeSet<>();
        Map<Integer, Card> rankToCard = new HashMap<>();
        for (Card card : totalHand) {
            int rank = card.getRank().ordinal();
            uniqueRanks.add(rank);
            rankToCard.put(rank, card);
            if (rank == 12) {
                uniqueRanks.add(-1);
                rankToCard.put(-1, card);
            }
        }
        List<Card> bestStraight = new ArrayList<>();
        LinkedList<Card> currentStraight = new LinkedList<>();
        List<Integer> descendingRanks = new ArrayList<>(uniqueRanks.descendingSet());
        for (int rank : descendingRanks) {
            if (currentStraight.isEmpty() || rank == currentStraight.getFirst().getRank().ordinal() - 1) {
                currentStraight.addFirst(rankToCard.get(rank));
            } else {
                currentStraight.clear();
                currentStraight.add(rankToCard.get(rank));
            }
            if (currentStraight.size() == 5) {
                bestStraight = new ArrayList<>(currentStraight);
            }
        }
        List<Integer> wheel = Arrays.asList(-1, 0, 1, 2, 3);
        if (uniqueRanks.containsAll(wheel) && !uniqueRanks.contains(4)) {
            bestStraight = Arrays.asList(
                rankToCard.get(-1), rankToCard.get(0), rankToCard.get(1), rankToCard.get(2), rankToCard.get(3)
            );
        }
        return bestStraight;
    }

    private synchronized static boolean isThreeOfAKind(List<Card> totalHand) {
        return countSameRank(totalHand, 3);
    }

    private synchronized static List<Card> getThreeOfAKindCards(List<Card> totalHand) {
        Map<Card.Rank, Integer> rankCount = new HashMap<>();
        for (Card card : totalHand) {
            rankCount.put(card.getRank(), rankCount.getOrDefault(card.getRank(), 0) + 1);
        }
        int threeOfSameRank = -1;
        for (Map.Entry<Card.Rank, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() == 3) {
                threeOfSameRank = entry.getKey().ordinal();
                break;
            }
        }
        if (threeOfSameRank == -1) return Collections.emptyList();

        List<Card> hand = new ArrayList<>();
        for (Card card : totalHand) {
            if (card.getRank().ordinal() == threeOfSameRank) {
                hand.add(card);
            }
        }
        List<Card> remainingCards = new ArrayList<>();
        for (Card card : totalHand) {
            if (card.getRank().ordinal() != threeOfSameRank) {
                remainingCards.add(card);
            }
        }
        List<Card> highCards = getHighCards(remainingCards);
        hand.add(highCards.get(0));
        hand.add(highCards.get(1));
        return hand;
    }

    private synchronized static int getValueThreeOfAKind(List<Card> totalHand) {
        if (!isThreeOfAKind(totalHand)) return 0;
        Map<Card.Rank, Integer> rankCount = new HashMap<>();
        for (Card card : totalHand) {
            rankCount.put(card.getRank(), rankCount.getOrDefault(card.getRank(), 0) + 1);
        }
        int sett = 0;
        for (Map.Entry<Card.Rank, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() == 3) {
                sett = entry.getKey().ordinal();
                break;
            }
        }
        List<Integer> kickers = new ArrayList<>();
        for (Card card : totalHand) {
            if (card.getRank().ordinal() != sett) {
                kickers.add(card.getRank().ordinal());
            }
        }
        Collections.sort(kickers, Collections.reverseOrder());
        return (sett * 100000) + (kickers.get(0) * 5000) + (kickers.get(1) * 350);
    }

    private synchronized static boolean isTwoPair(List<Card> totalHand) {
        return countPairs(totalHand) >= 2;
    }

    private synchronized static List<Card> getTwoPairsCards(List<Card> totalHand) {
        Map<Card.Rank, Integer> rankCount = new HashMap<>();
        for (Card card : totalHand) {
            rankCount.put(card.getRank(), rankCount.getOrDefault(card.getRank(), 0) + 1);
        }
        List<Integer> pairs = new ArrayList<>();
        for (Map.Entry<Card.Rank, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() == 2) {
                pairs.add(entry.getKey().ordinal());
            }
        }
        Collections.sort(pairs, Collections.reverseOrder());
        int firstPair = pairs.get(0);
        int secondPair = pairs.get(1);

        List<Card> hand = new ArrayList<>();
        for (Card card : totalHand) {
            if (card.getRank().ordinal() == firstPair) {
                hand.add(card);
            }
        }
        for (Card card : totalHand) {
            if (card.getRank().ordinal() == secondPair) {
                hand.add(card);
            }
        }
        List<Card> remainingCards = new ArrayList<>();
        for (Card card : totalHand) {
            if (card.getRank().ordinal() != firstPair && card.getRank().ordinal() != secondPair) {
                remainingCards.add(card);
            }
        }
        List<Card> highCards = getHighCards(remainingCards);
        hand.add(highCards.get(0));
        return hand;
    }

    private synchronized static int getTwoPairValue(List<Card> totalHand) {
        if (!isTwoPair(totalHand)) return 0;
        Map<Card.Rank, Integer> rankCount = new HashMap<>();
        for (Card card : totalHand) {
            rankCount.put(card.getRank(), rankCount.getOrDefault(card.getRank(), 0) + 1);
        }
        List<Integer> pairs = new ArrayList<>();
        for (Map.Entry<Card.Rank, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() == 2) {
                pairs.add(entry.getKey().ordinal());
            }
        }
        Collections.sort(pairs, Collections.reverseOrder());
        int firstPair = pairs.get(0);
        int secondPair = pairs.get(1);

        int kicker = 0;
        for (Card card : totalHand) {
            int rank = card.getRank().ordinal();
            if (rank != firstPair && rank != secondPair) {
                if (rank > kicker) kicker = rank;
            }
        }
        return (firstPair * 100000) + (secondPair * 1000) + (kicker * 10);
    }

    private synchronized static boolean isOnePair(List<Card> totalHand) {
        return countPairs(totalHand) == 1;
    }

    private synchronized static List<Card> getOnePairCards(List<Card> totalHand) {
        Map<Card.Rank, Integer> rankCount = new HashMap<>();
        for (Card card : totalHand) {
            rankCount.put(card.getRank(), rankCount.getOrDefault(card.getRank(), 0) + 1);
        }
        int sameRank = -1;
        for (Map.Entry<Card.Rank, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() == 2) {
                sameRank = entry.getKey().ordinal();
                break;
            }
        }
        if (sameRank == -1) return Collections.emptyList();

        List<Card> hand = new ArrayList<>();
        for (Card card : totalHand) {
            if (card.getRank().ordinal() == sameRank) {
                hand.add(card);
            }
        }
        List<Card> remainingCards = new ArrayList<>();
        for (Card card : totalHand) {
            if (card.getRank().ordinal() != sameRank) {
                remainingCards.add(card);
            }
        }
        List<Card> highCards = getHighCards(remainingCards);
        hand.add(highCards.get(0));
        hand.add(highCards.get(1));
        hand.add(highCards.get(2));
        return hand;
    }

    private synchronized static int getOnePairValue(List<Card> totalHand) {
        if (!isOnePair(totalHand)) return 0;
        Map<Card.Rank, Integer> rankCount = new HashMap<>();
        for (Card card : totalHand) {
            rankCount.put(card.getRank(), rankCount.getOrDefault(card.getRank(), 0) + 1);
        }
        int pairRank = 0;
        for (Map.Entry<Card.Rank, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() == 2) {
                pairRank = entry.getKey().ordinal();
                break;
            }
        }
        List<Integer> kickers = new ArrayList<>();
        for (Card card : totalHand) {
            if (card.getRank().ordinal() != pairRank) {
                kickers.add(card.getRank().ordinal());
            }
        }
        Collections.sort(kickers, Collections.reverseOrder());
        return (pairRank * 100000) + (kickers.get(0) * 5000) + (kickers.get(1) * 350) + (kickers.get(2) * 10);
    }

    private synchronized static int getHighCardsValue(List<Card> totalHand) {
        List<Integer> sortedRanks = new ArrayList<>();
        for (Card card : totalHand) {
            sortedRanks.add(card.getRank().ordinal());
        }
        Collections.sort(sortedRanks, Collections.reverseOrder());
        List<Integer> highCards = sortedRanks.subList(0, Math.min(5, sortedRanks.size()));
        return highCards.get(0) * 10000 + highCards.get(1) * 10000 + highCards.get(2) * 1000 + highCards.get(3) * 10 + highCards.get(4);
    }

    private synchronized static List<Card> getHighCards(List<Card> totalHand) {
        List<Card> sortedCards = new ArrayList<>(totalHand);
        Collections.sort(sortedCards, new Comparator<Card>() {
            @Override
            public int compare(Card c1, Card c2) {
                return c2.getRank().ordinal() - c1.getRank().ordinal();
            }
        });
        return sortedCards.subList(0, Math.min(5, sortedCards.size()));
    }
}
