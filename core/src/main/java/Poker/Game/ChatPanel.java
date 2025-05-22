package Poker.Game;

import Poker.Game.PacketsClasses.ChatMessage;
import Poker.Game.PacketsClasses.GameStartedNotification;
import Poker.Game.PacketsClasses.GameStats;
import Poker.Game.PacketsClasses.PlayerJoinedNotification;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Align;


import Poker.Game.Client.PokerClient;

public class ChatPanel extends Table implements ChatListener {
    private static final int MAX_MESSAGES = 100;

    private final Table messagesTable;
    private final ScrollPane scroll;
    private final TextField inputField;
    private final TextButton sendButton;
    private final Label.LabelStyle chatLabelStyle;
    private final Label.LabelStyle sysLabelStyle;

    private boolean needsLayout = false;
    private boolean userScrolledUp = false;

    public ChatPanel(Skin skin, PokerClient client, float uiScale) {
        super(skin);

        // Стили сообщений
        Label.LabelStyle base = skin.get("default", Label.LabelStyle.class);
        chatLabelStyle = new Label.LabelStyle(base);
        chatLabelStyle.fontColor = Color.WHITE;
        sysLabelStyle = new Label.LabelStyle(base);
        sysLabelStyle.fontColor = Color.WHITE;

        // Таблица сообщений
        messagesTable = new Table();
        messagesTable.top().left();
        messagesTable.defaults().pad(uiScale).left().width(380f * uiScale);

        // ScrollPane
        scroll = new ScrollPane(messagesTable, skin);
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);
        scroll.setForceScroll(false, true);
        scroll.setScrollbarsOnTop(true);
        scroll.setScrollBarPositions(true, true); // справа, сверху

        // Ловим ручную прокрутку пользователя
        scroll.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float scrollY = scroll.getScrollPercentY();
                userScrolledUp = scrollY < 0.98f;
            }
        });



        // Поле ввода и кнопка
        inputField = new TextField("", skin);
        inputField.setMessageText("Type a message...");
        sendButton = new TextButton("Send", skin);
        sendButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String msg = inputField.getText().trim();
                if (!msg.isEmpty()) {
                    client.sendChatMessage(msg);
                    inputField.setText("");
                }
            }
        });

        // Layout
        pad(uiScale);
        defaults().space(uiScale);
        add(scroll).colspan(2).width(460f * uiScale).height(250f * uiScale).row();
        add(inputField).expandX().fillX().height(40f * uiScale);
        add(sendButton).width(80f * uiScale).height(40f * uiScale);
        align(Align.bottomLeft);

        // Подписка на входящие
        client.onChatMessage(this);
    }

    public void addMessage(String text) {
        if (text == null || text.trim().isEmpty()) return;

        boolean isSys = text.startsWith("sys:");
        String display = isSys ? text.substring(Math.min(4, text.length())).trim() : text;
        if (display.isEmpty()) return; // Не добавляем пустые строки

        Label.LabelStyle style = isSys ? sysLabelStyle : chatLabelStyle;

        Label lbl = new Label(display, style);
        lbl.setWrap(false);
        messagesTable.add(lbl)
            .width(380f * Gdx.graphics.getDensity()) // можно заменить на uiScale
            .left()
            .row();

        if (messagesTable.getChildren().size > MAX_MESSAGES) {
            messagesTable.getChildren().removeIndex(0);
        }

        needsLayout = true;
    }


    public void updateLayoutIfNeeded() {
        if (needsLayout && !userScrolledUp) {
            // 1) Обновляем таблицу сообщений
            messagesTable.invalidateHierarchy();
            scroll.updateVisualScroll();
            // ← вместо scroll.updateVisualScroll()

            // 2) Отложенный scroll вниз
            Gdx.app.postRunnable(() -> scroll.setScrollPercentY(1f));

            needsLayout = false;
        }
    }


    // Обработка входящих чатов
    @Override
    public void onChatMessage(ChatMessage msg) {
        String prefix = msg.getName().equals("sys") ? "sys:" : msg.getName() + ":";
        String full = prefix + " " + msg.getMessage();

        Gdx.app.postRunnable(() -> addMessage(full));
    }

    // Другие события игры
    @Override public void onGameStarted(GameStartedNotification note) {
        addMessage("sys: Game Started!");
    }

    @Override public void onGameStats(GameStats object) {
        addMessage("sys: " + object.getStats());
    }

    @Override public void onPlayerJoinedNotification(PlayerJoinedNotification object) {
        addMessage("sys: Player " + object.nickname + " joined the game.");
    }
}
