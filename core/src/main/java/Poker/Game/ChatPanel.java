package Poker.Game;

import Poker.Game.PacketsClasses.ChatMessage;
import Poker.Game.PacketsClasses.GameStartedNotification;
import Poker.Game.PacketsClasses.PlayerJoinedNotification;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
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
        // Styles
        Label.LabelStyle base = skin.get("default", Label.LabelStyle.class);
        chatLabelStyle = new Label.LabelStyle(base);
        chatLabelStyle.fontColor = Color.WHITE;
        sysLabelStyle = new Label.LabelStyle(base);
        sysLabelStyle.fontColor = Color.WHITE;

        // Message table
        messagesTable = new Table();
        messagesTable.top().left();
        messagesTable.defaults().pad(uiScale).left().width(380f * uiScale);

        // ScrollPane
        scroll = new ScrollPane(messagesTable, skin);
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);
        scroll.setForceScroll(false, true);
        scroll.setScrollbarsOnTop(true);
        scroll.setScrollBarPositions(true, true);
        scroll.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float scrollY = scroll.getScrollPercentY();
                userScrolledUp = scrollY < 0.98f;
            }
        });

        // Input
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

        client.onChatMessage(this);
    }

    private static String filterUnsupportedChars(String input, BitmapFont font) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0, n = input.length(); i < n; i++) {
            char c = input.charAt(i);
            if (font.getData().getGlyph(c) != null) {
                sb.append(c);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }
    public void addMessage(String text) {
        if (text == null) return;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return;
        boolean isSys = trimmed.startsWith("sys:");
        String content = isSys ? trimmed.substring(4).trim() : trimmed;
        content = filterUnsupportedChars(content, chatLabelStyle.font);
        if (content.isEmpty()) return;
        Label.LabelStyle style = isSys ? sysLabelStyle : chatLabelStyle;
        try {
            Label lbl = new Label(content, style);
            lbl.setWrap(true);
            messagesTable.add(lbl)
                .width(380f * UIScale.ui)
                .left()
                .row();
            // Trim old messages
            if (messagesTable.getChildren().size > MAX_MESSAGES) {
                Actor firstActor = messagesTable.getChildren().first();
                messagesTable.removeActor(firstActor); // корректно удаляет и Actor, и Cell
            }
            needsLayout = true;
        } catch (Exception e) {
            Gdx.app.error("SafeChatPanel", "addMessage failure: " + content, e);
        }
    }

    public void updateLayoutIfNeeded() {
        if (needsLayout && !userScrolledUp) {
            try {
                messagesTable.invalidateHierarchy();
                scroll.updateVisualScroll();
                Gdx.app.postRunnable(() -> scroll.scrollTo(0, 0, 0, 0));
            } catch (Exception e) {
                Gdx.app.error("SafeChatPanel", "updateLayout failure", e);
            }
            needsLayout = false;
        }
    }

    @Override
    public void onChatMessage(ChatMessage msg) {
        String prefix = msg.getName().equals("sys") ? "sys:" : msg.getName() + ":";
        String full = prefix + " " + msg.getMessage();
        Gdx.app.postRunnable(() -> addMessage(full));
    }

    @Override
    public void onGameStarted(GameStartedNotification note) {
        addMessage("sys: Game Started!");
    }
    @Override
    public void onPlayerJoinedNotification(PlayerJoinedNotification note) {
        addMessage(note.nickname + " joined the game.");
    }
}
