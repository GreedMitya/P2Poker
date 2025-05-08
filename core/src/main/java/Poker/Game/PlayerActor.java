package Poker.Game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;

import java.util.ArrayList;
import java.util.List;

public class PlayerActor extends WidgetGroup {
    private final int playerId;
    private final Skin skin;
    private final Image avatarImage;
    private final Label nameLabel;
    private final Label balanceLabel;
    private final List<CardActor> backActors = new ArrayList<>();
    private Label betLabel;

    private final float avatarSize = 64;
    private final float cardWidth = 60, cardHeight = 82;
    private final float betLabelOffsetX = -35; // для позиционирования ставки слева от аватара
    private final float avatarOffsetX = 0;  // позиция аватара
    private final float cardSpacing = 5; // расстояние между картами

    public PlayerActor(int playerId, String nickname, double balance, Texture avatarTexture, Skin skin) {
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
        clearCardBacks();
        float cardStartX = avatarOffsetX-10; // Начальная позиция для карт
        float cardStartY = 25; // Позиция карт над аватаром

        // Создаём карты
        for (int i = 0; i < 2; i++) {
            CardActor back = new CardActor(null);
            back.setSize(cardWidth, cardHeight);
            back.setFaceDown(true);
            back.setPosition(cardStartX + i * (cardWidth -35), cardStartY);
            backActors.add(back);
            addActor(back);
        }
    }

    public void clearCardBacks() {
        for (CardActor ca : backActors) {
            ca.setVisible(false); // Прячем карты, но не удаляем их
        }
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
}
