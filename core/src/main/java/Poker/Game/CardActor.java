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
            this.faceTexture = null; // Для рубашки
            setSize(backTexture.getRegionWidth(), backTexture.getRegionHeight());
            faceDown = true;
        }
    }

    // Новые методы для «переворота» карты
    public void showBack() {
        setFaceDown(true);
    }

    public void showFront() {
        setFaceDown(false);
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
            this.setColor(Color.GOLD);
        } else {
            this.setColor(Color.WHITE);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.setColor(getColor().r, getColor().g, getColor().b, parentAlpha);
        TextureRegion tex = faceDown ? backTexture : faceTexture;
        if (tex != null) {
            batch.draw(tex, getX(), getY(), getOriginX(), getOriginY(),
                getWidth(), getHeight(), getScaleX(), getScaleY(), getRotation());
        }
    }
}


