package GreedMitya.P2Poker;

import GreedMitya.P2Poker.PacketsClasses.ChatMessage;
import GreedMitya.P2Poker.PacketsClasses.GameStartedNotification;
import GreedMitya.P2Poker.PacketsClasses.PlayerJoinedNotification;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import GreedMitya.P2Poker.Client.PokerClient;

import static com.badlogic.gdx.Gdx.app;

public class ChatPanel extends Table implements ChatListener {
    private SoundManager soundManager;

    private static final int MAX_MESSAGES = 100;
    private final Table messagesTable;
    private final ScrollPane scroll;
    private final TextField inputField;
    private final TextButton sendButton;
    private final Label.LabelStyle chatLabelStyle;
    private final Label.LabelStyle sysLabelStyle;
    private boolean needsLayout = false;
    private boolean userScrolledUp = false;
    public void setSoundManager(SoundManager soundManager) {
        this.soundManager = soundManager;
    }

    public ChatPanel(Skin skin, PokerClient client, float uiScale) {
        super(skin);

        Label.LabelStyle base = skin.get("default", Label.LabelStyle.class);
        chatLabelStyle = new Label.LabelStyle(base);
        chatLabelStyle.fontColor = Color.WHITE;
        sysLabelStyle = new Label.LabelStyle(base);
        sysLabelStyle.fontColor = Color.WHITE;


        messagesTable = new Table();
        messagesTable.top().left();
        messagesTable.defaults().pad(uiScale).left().width(380f * uiScale);


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

            if (messagesTable.getChildren().size > MAX_MESSAGES) {
                Actor firstActor = messagesTable.getChildren().first();
                messagesTable.removeActor(firstActor);
            }
            needsLayout = true;
        } catch (Exception e) {
            app.error("SafeChatPanel", "addMessage failure: " + content, e);
        }
    }

    public void updateLayoutIfNeeded() {
        if (needsLayout && !userScrolledUp) {
            try {
                messagesTable.invalidateHierarchy();
                scroll.updateVisualScroll();
                app.postRunnable(() -> scroll.scrollTo(0, 0, 0, 0));
            } catch (Exception e) {
                app.error("SafeChatPanel", "updateLayout failure", e);
            }
            needsLayout = false;
        }
    }

    @Override
    public void onChatMessage(ChatMessage msg) {
        String prefix = msg.getName().equals("sys") ? "sys:" : msg.getName() + ":";
        String full = prefix + " " + msg.getMessage();
        app.postRunnable(() -> addMessage(full));
    }

    @Override
    public void onGameStarted(GameStartedNotification note) {
        addMessage("sys: Game Started!");
    }
    @Override
    public void onPlayerJoinedNotification(PlayerJoinedNotification note) {
        addMessage(note.nickname + " joined the game.");
        SoundManager.getInstance().play("enterance", 1f);

    }
}
