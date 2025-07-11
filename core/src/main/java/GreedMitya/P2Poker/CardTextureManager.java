package GreedMitya.P2Poker;

import com.badlogic.gdx.graphics.Texture;

import GreedMitya.P2Poker.core.Card;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;
import java.util.Map;


public class CardTextureManager {
    private static final Map<String, TextureRegion> cardTextures = new HashMap<>();
    private static TextureRegion backTexture;

    public static void load() {
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                String key = getFileName(rank, suit);
                String path = "sgx/exported_cards/" + key;
                Texture texture = new Texture(Gdx.files.internal(path));
                cardTextures.put(key, new TextureRegion(texture));
            }
        }
        Texture back = new Texture(Gdx.files.internal("sgx/exported_cards/back.png"));
        backTexture = new TextureRegion(back);
    }
    public static TextureRegion getBackTexture() {
        return backTexture;
    }

    public static TextureRegion getTexture(Card card) {
        return cardTextures.get(getFileName(card.getRank(), card.getSuit()));
    }

    private static String getFileName(Card.Rank rank, Card.Suit suit) {
        return getRankShort(rank) + getSuitShort(suit) + ".png";
    }

    private static String getRankShort(Card.Rank rank) {
        switch (rank) {
            case Ace: return "A";
            case King: return "K";
            case Queen: return "Q";
            case Jack: return "J";
            case Ten: return "10";
            case Nine: return "9";
            case Eight: return "8";
            case Seven: return "7";
            case Six: return "6";
            case Five: return "5";
            case Four: return "4";
            case Three: return "3";
            case Two: return "2";
        }
        return "X";
    }

    private static String getSuitShort(Card.Suit suit) {
        switch (suit) {
            case Spades: return "Spades";
            case Hearts: return "Hearts";
            case Diamonds: return "Diamonds";
            case Clubs: return "Clubs";
        }
        return "X";
    }


}
