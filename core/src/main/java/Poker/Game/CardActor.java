package Poker.Game;

import Poker.Game.core.Card;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class CardActor extends Actor {
    private final Card card;
    private final TextureRegion faceTexture;
    private static TextureRegion backTexture;
    private boolean faceDown = false;

    public CardActor(Card card) {
        this.card = card;

        if (backTexture == null) {
            backTexture = CardTextureManager.getBackTexture();
        }

        if (card != null) {
            this.faceTexture = CardTextureManager.getTexture(card);
            setSize(faceTexture.getRegionWidth(), faceTexture.getRegionHeight());
        } else {
            this.faceTexture = null; // Не нужен, мы рисуем только рубашку
            setSize(backTexture.getRegionWidth(), backTexture.getRegionHeight());
            faceDown = true; // рубашка по умолчанию
        }
    }

    public void setFaceDown(boolean faceDown) {
        this.faceDown = faceDown;
    }

    public boolean isFaceDown() {
        return faceDown;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {

        batch.setColor(1, 1, 1, parentAlpha);
        TextureRegion texture = faceDown ? backTexture : faceTexture;
        if (texture != null) {
            batch.draw(texture, getX(), getY(), getWidth(), getHeight());
        }
    }
}


