package GreedMitya.P2Poker;

import GreedMitya.P2Poker.core.Card;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class CardActor extends Actor {
    private Card card;
    private TextureRegion faceTexture;
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
    public void setDimmed(boolean dimmed) {
        if (dimmed) {
            this.setColor(0.5f, 0.5f, 0.5f, 1f); // серый цвет
        } else {
            this.setColor(1f, 1f, 1f, 1f); // нормальный цвет
        }
    }


    // Новые методы для «переворота» карты
    public void showBack() {
        setFaceDown(true);
    }

    public void showFront() {
        setFaceDown(false);
    }
    public void setCard(Card card) {
        this.card = card;
        this.faceTexture = CardTextureManager.getTexture(card);
    }
    public void setFaceDown(boolean faceDown) {
        this.faceDown = faceDown;
        // Увеличиваем размер карт
        setSize(UIConfig.CARD_BASE_WIDTH*UIScale.ui, UIConfig.CARD_BASE_HEIGHT*UIScale.ui); // Примерно на 1.5x больше
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


