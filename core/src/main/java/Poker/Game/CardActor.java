package Poker.Game;

import Poker.Game.core.Card;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class CardActor extends Actor {
    private final Card card;
    private final TextureRegion faceTexture;
    private static TextureRegion backTexture;
    private boolean faceDown = false;
    private boolean highlighted = false;

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
    public Card getCard() {
        return card;
    }
    public void setHighlight(boolean highlight) {
        this.highlighted = highlight;
        if (highlight) {
            this.setColor(Color.GOLD); // Подсветка жёлтым
        } else {
            this.setColor(Color.WHITE); // Обычный цвет
        }
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


