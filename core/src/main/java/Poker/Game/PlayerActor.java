package Poker.Game;

import Poker.Game.core.Card;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;

import java.util.ArrayList;
import java.util.List;

public class PlayerActor extends WidgetGroup {
    private final boolean isLocalPlayer;
    private static final float CARD_WIDTH  = 90f;
    private static final float CARD_HEIGHT = 134f;
    private static final float CARD_GAP    = 50f;
    private final int playerId;
    private final Skin skin;
    private final Image avatarImage;
    private final Label nameLabel;
    private final Label balanceLabel;
    private final List<CardActor> backActors = new ArrayList<>();
    private final List<CardActor> handCardActors = new ArrayList<>();
    private Label betLabel;

    private final float avatarSize = 128;
    private final float cardWidth = 60, cardHeight = 82;
    private final float betLabelOffsetX = -35; // для позиционирования ставки слева от аватара
    private final float avatarOffsetX = 0;  // позиция аватара
    private final float cardSpacing = 5; // расстояние между картами

    public PlayerActor(boolean isLocalPlayer, int playerId, String nickname, double balance, Texture avatarTexture, Skin skin) {
        this.isLocalPlayer = isLocalPlayer;
        this.playerId = playerId;
        this.skin = skin;

        // Аватар
        avatarImage = new Image(avatarTexture);
        avatarImage.setSize(avatarSize, avatarSize);
        avatarImage.setPosition(avatarOffsetX, -15); // Позиция аватара
        addActor(avatarImage);

        // Ник
        nameLabel = new Label(nickname, skin);
        nameLabel.setFontScale(0.8f);
        nameLabel.setPosition(avatarOffsetX, -30); // Под аватаром
        addActor(nameLabel);

        // Баланс
        balanceLabel = new Label((int) balance + "$", skin);
        balanceLabel.setFontScale(0.8f);
        balanceLabel.setPosition(avatarOffsetX, -45); // Под ником
        addActor(balanceLabel);
    }

    public void showCardBacks() {
        if (isLocalPlayer || !handCardActors.isEmpty()) {
            return;
        }
        clearCardBacks();
        float cardStartX = avatarOffsetX; // Начальная позиция для карт
        float cardStartY = 25; // Позиция карт над аватаром

        // Создаём карты
        for (int i = 0; i < 2; i++) {
            CardActor back = new CardActor(null);
            back.setSize(CARD_WIDTH, CARD_HEIGHT);
            back.setFaceDown(true);
            back.setPosition(cardStartX + i * (cardWidth), cardStartY);
            backActors.add(back);
            addActor(back);
        }
    }

    public void clearCardBacks() {
        for (CardActor ca : backActors) {
            ca.remove();   // удаляем из сцены
        }
        backActors.clear();
    }

    public void showHandCards(List<Card> hand) {
        clearCardBacks();
        clearHandCards();
        float cardStartX = avatarOffsetX - 10;
        float cardStartY = 25;

        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            CardActor actor = new CardActor(card);
            actor.setSize(CARD_WIDTH, CARD_HEIGHT);
            actor.showFront();
            actor.setPosition(cardStartX + i * (cardWidth - 35), cardStartY);
            handCardActors.add(actor);
            addActor(actor);
        }
    }
    public void highlightWinningCards(List<Card> winningCards) {
        Color glowColor = new Color(1, 1, 0, 0.6f);
        for (CardActor card : getCardActors()) {
            if (winningCards.contains(card.getCard())) {
                card.addAction(Actions.sequence(
                    Actions.color(glowColor, 0.3f),
                    Actions.delay(1f),
                    Actions.color(Color.WHITE, 0.3f)
                ));
            }
        }
    }

    public void clearHandCards() {
        for (CardActor card : handCardActors) {
            card.remove();
        }
        handCardActors.clear();
    }
    public List<CardActor> getCardActors() {
        return handCardActors;
    }
    public void updateBalance(double newBalance) {
        balanceLabel.setText((int) newBalance + "$");
    }

    public void showBet(double amount) {
        clearBet();
        if (amount <= 0) return;

        Label.LabelStyle betStyle = new Label.LabelStyle(skin.getFont("font"), Color.WHITE);
        betStyle.background = skin.newDrawable("white", Color.DARK_GRAY);
        betLabel = new Label((int) amount + "$", betStyle);
        betLabel.setFontScale(1.2f);
        betLabel.setColor(Color.GOLD);
        betLabel.setAlignment(Align.center);

        // Добавляем ставку слева от аватара
        betLabel.setPosition(betLabelOffsetX, -3);
        addActor(betLabel);
    }

    public void clearBet() {
        if (betLabel != null) {
            betLabel.remove();
            betLabel = null;
        }
    }

    @Override
    public float getPrefWidth() {
        return 200;  // Определяем размер по своему усмотрению
    }

    @Override
    public float getPrefHeight() {
        return 200;  // Определяем размер по своему усмотрению
    }

    public boolean isLocalPlayer() {
        return isLocalPlayer;
    }
}
