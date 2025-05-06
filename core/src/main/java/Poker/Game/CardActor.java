package Poker.Game;

import Poker.Game.core.Card;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;

import java.util.HashMap;
import java.util.Map;

public class CardActor extends Actor {
    private final Card card;
    private final TextureRegion texture;

    public CardActor(Card card) {
        this.card = card;
        this.texture = CardTextureManager.getTexture(card); // <-- важно!
        setSize(texture.getRegionWidth(), texture.getRegionHeight());
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.setColor(1, 1, 1, parentAlpha);
        batch.draw(texture, getX(), getY(), getWidth(), getHeight());
    }
}

