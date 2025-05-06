package Poker.Game.core;

import java.util.HashMap;
import java.util.Map;

public class Card implements Comparable<Card>{
    private Rank rank; // Ранг карты
    private Suit suit; // Ее Масть


    //Делаем конструктор карты
    public Card (Rank rank, Suit suit){
        this.rank = rank;
        this.suit = suit;
    }
    public Card(){

    }
    @Override
    public int hashCode() {
        return java.util.Objects.hash(rank, suit);
    }


    public Suit getSuit() {
        return suit;
    }
    public Rank getRank() {
        return rank;
    }
    // Делаем метод отображения карты через toString()
    private static final Map<Suit, String> suitSymbols = new HashMap<>();
    private static final Map<Card.Rank, String> rankSymbols = new HashMap<>();

    static {
        suitSymbols.put(Card.Suit.Hearts, "♥️");
        suitSymbols.put(Card.Suit.Diamonds, "♦️");
        suitSymbols.put(Card.Suit.Clubs, "♣️");
        suitSymbols.put(Card.Suit.Spades, "♠️");

        rankSymbols.put(Card.Rank.Ace, "A");
        rankSymbols.put(Card.Rank.King, "K");
        rankSymbols.put(Card.Rank.Queen, "Q");
        rankSymbols.put(Card.Rank.Jack, "J");
        rankSymbols.put(Card.Rank.Ten, "10");
        rankSymbols.put(Card.Rank.Nine, "9");
        rankSymbols.put(Card.Rank.Eight, "8");
        rankSymbols.put(Card.Rank.Seven, "7");
        rankSymbols.put(Card.Rank.Six, "6");
        rankSymbols.put(Card.Rank.Five, "5");
        rankSymbols.put(Card.Rank.Four, "4");
        rankSymbols.put(Card.Rank.Three, "3");
        rankSymbols.put(Card.Rank.Two, "2");
    }

    @Override
    public String toString() {
        return rankSymbols.get(rank) + suitSymbols.get(suit);
    }


    public boolean equals(Object object) { // сравнение обьектов
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        Card card = (Card) object; // преобразование  обьекта в карту
        return this.rank == card.rank && this.suit == card.suit; // Сама проверка двух карт
    }

    @Override
    public int compareTo(Card other) {
        // Сначала сравниваем ранги
        int rankComparison = Integer.compare(this.rank.ordinal(), other.rank.ordinal());
        if (rankComparison != 0) {
            return rankComparison; // Если ранги разные, возвращаем результат сравнения
        }

        // Если ранги одинаковые, сравниваем масти
        return this.suit.ordinal() - other.suit.ordinal();
    }



    public enum Suit {  // Enum простой способ хранить фиксированные данные!(Набор фиксированных констант)
        Hearts,Spades,Diamonds,Clubs
    } // Надо придумать как конвертировать текст потом в удобные значки
    public enum Rank { // У карты будет два свойства SUIT и RANK.
        Two,Three,Four,Five,Six,Seven,Eight,Nine,Ten,Jack,Queen,King,Ace
    }

}
