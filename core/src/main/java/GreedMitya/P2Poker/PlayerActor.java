package GreedMitya.P2Poker;

import GreedMitya.P2Poker.core.Card;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;

import java.util.ArrayList;
import java.util.List;

public class PlayerActor extends WidgetGroup {
    private int tableSide = -1;
    private final float avatarSize   = 64f  * UIScale.ui;
    private final float cardWidth    = 75f  * UIScale.ui;
    private final float cardHeight   = 120f  * UIScale.ui;
    private final float cardSpacing  =  0f  * UIScale.ui;
    private final Skin skin;
    private final Table frameTable;
    private final Image avatarImage;
    private final Label nameLabel;
    private final Label balanceLabel;
    private final List<CardActor> handCardActors = new ArrayList<>();
    private final List<CardActor> backActors = new ArrayList<>();
    private Label betLabel;
    private final boolean isLocalPlayer;
    private final int playerId;
    private double currentBetAmount = 0;
    private Image dealerButton;


    public double getCurrentBetAmount() {
        return currentBetAmount;
    }


    public PlayerActor(boolean isLocalPlayer, int playerId, String nickname, double balance, Texture avatarTexture, Skin skin) {
        this.skin = skin;
        this.playerId = playerId;
        this.isLocalPlayer = isLocalPlayer;

        frameTable = new Table(skin);
        frameTable.pad(6);
        frameTable.left().top();
        frameTable.defaults().expand(false, false);

        avatarImage = new Image(avatarTexture);
        avatarImage.setSize(avatarSize, avatarSize);

        nameLabel = new Label(nickname, skin);
        nameLabel.setFontScale(0.9f);
        nameLabel.setColor(Color.WHITE);

        balanceLabel = new Label((int) balance + "$", skin);
        balanceLabel.setFontScale(0.8f);
        balanceLabel.setColor(Color.valueOf("7CFC00"));


        Table infoTable = new Table();
        infoTable.add(nameLabel).left().row();
        infoTable.add(balanceLabel).left();

        frameTable.add(avatarImage).size(avatarSize).padRight(8);
        frameTable.add(infoTable).left();
        frameTable.pack();

        addActor(frameTable);
    }

    @Override
    public void layout() {
        super.layout();
        frameTable.setPosition(0, 0);

        float totalWidth = backActors.isEmpty()
            ? handCardActors.size() * cardWidth + (handCardActors.size()-1)*cardSpacing
            : backActors.size()     * cardWidth + (backActors.size()-1)*cardSpacing;

        float avatarX = avatarImage.getX(), avatarY = avatarImage.getY();
        float avatarW = avatarImage.getWidth(), avatarH = avatarImage.getHeight();
        float startX  = avatarX + avatarW/2f - totalWidth/2f;
        float cardY   = frameTable.getTop() + 5f;

        for (int i = 0; i < handCardActors.size(); i++) {
            Actor c = handCardActors.get(i);
            c.setPosition(startX + i*(cardWidth+cardSpacing), cardY);
        }
        for (int i = 0; i < backActors.size(); i++) {
            Actor c = backActors.get(i);
            c.setPosition(startX + i*(cardWidth+cardSpacing), cardY);
        }

        if (betLabel != null) {
            float labelX = avatarX - betLabel.getWidth() - 8f;
            float labelY = avatarY + avatarH/2f - betLabel.getHeight()/2f;
            betLabel.setPosition(labelX, labelY);
        }

        if (dealerButton != null) {
            float x = 0, y = 0;
            float margin = 8f;

            switch (tableSide) {
                case 0: // низ
                    x = avatarX - avatarW - dealerButton.getWidth() / 1.7f;
                    y = avatarY + 2*dealerButton.getHeight() + 2*margin;
                    break;
                case 1: // право
                    x = avatarX - avatarW/2f;
                    y = avatarY - avatarH/2f - dealerButton.getHeight() / 2f;
                    break;
                case 2: // верх
                    x = avatarX - avatarW/2 - dealerButton.getWidth() / 2f;
                    y = avatarY - avatarH/2;
                    break;
                case 3: // лево
                    x = avatarX + 2*dealerButton.getWidth() + 2*margin;
                    y = avatarY - avatarH/2f;
                    break;
                default:
                    x = avatarX + avatarW - dealerButton.getWidth() / 2f;
                    y = avatarY + avatarH - dealerButton.getHeight() / 2f;
            }

            dealerButton.setPosition(x, y);
        }
    }

    public void setTableSide(int side) {
        this.tableSide = side;
    }

    public int getTableSide() {
        return tableSide;
    }
    public void setDealer(boolean isDealer) {
        if (isDealer) {
            if (dealerButton == null) {
                Texture dealerTexture = new Texture("sgx/raw/DealerButton.png");
                dealerButton = new Image(dealerTexture);
                dealerButton.setSize(44f * UIScale.ui, 36f * UIScale.ui);
                addActor(dealerButton);
            }
        } else if (dealerButton != null) {
            dealerButton.remove();
            dealerButton = null;
        }
        invalidate();
    }


    public void showHandCards(List<Card> hand) {
        clearHandCards();
        clearCardBacks();

        for (Card card : hand) {
            CardActor actor = new CardActor(card);
            actor.setSize(cardWidth, cardHeight);
            actor.showFront();
            addActor(actor);
            handCardActors.add(actor);
        }
        invalidate();
    }
    public void showCardBacks() {
        if (isLocalPlayer || !handCardActors.isEmpty()) return;
        clearCardBacks();

        for (int i = 0; i < 2; i++) {
            CardActor back = new CardActor(null);
            back.setSize(cardWidth, cardHeight);
            back.setFaceDown(true);
            addActor(back);
            backActors.add(back);
        }
        invalidate();
    }
    public void clearCardBacks() {
        for (CardActor ca : backActors) {
            ca.remove();
        }
        backActors.clear();
    }
    public List<CardActor> getBackActors() {
        return backActors;
    }

    public void clearHandCards() {
        for (CardActor ca : handCardActors) {
            ca.remove();
        }
        handCardActors.clear();
    }
    public void dimCards() {
        for (CardActor card : handCardActors) {
            card.setDimmed(true);
        }
    }

    public void updateBalance(double newBalance) {
        balanceLabel.setText((int) newBalance + "$");
    }

    public void showBet(double amount) {
        clearBet();
        currentBetAmount = amount;
        if (amount <= 0) return;
        Label.LabelStyle betStyle = new Label.LabelStyle(skin.getFont("font"), Color.WHITE);
        betStyle.background = skin.newDrawable("white", Color.DARK_GRAY);
        betLabel = new Label((int) amount + "$", betStyle);
        betLabel.setFontScale(1.2f);
        betLabel.setColor(Color.GOLD);
        betLabel.setAlignment(Align.left);

        addActor(betLabel);
        invalidate();
    }

    public void clearBet() {
        if (betLabel != null) {
            betLabel.remove();
            betLabel = null;
        }
        currentBetAmount = 0;
    }

    @Override
    public float getPrefWidth() {
        return frameTable.getPrefWidth();
    }

    @Override
    public float getPrefHeight() {
        return frameTable.getPrefHeight() + cardHeight + 15f;
    }

    public List<CardActor> getCardActors() {
        return handCardActors;
    }

    public boolean isLocalPlayer() {
        return isLocalPlayer;
    }

    public int getPlayerId() {
        return playerId;
    }
    public Label createFlyingBetLabel(double amount) {
        Label.LabelStyle betStyle = new Label.LabelStyle(skin.getFont("font"), Color.WHITE);
        betStyle.background = skin.newDrawable("white", Color.DARK_GRAY);
        Label flying = new Label( amount + "$", betStyle);
        flying.setFontScale(1.2f);
        flying.setColor(Color.GOLD);

        Vector2 from = localToStageCoordinates(new Vector2(
            getWidth() / 2f,
            getHeight() / 2f
        ));
        flying.setPosition(from.x, from.y);
        return flying;
    }

}
