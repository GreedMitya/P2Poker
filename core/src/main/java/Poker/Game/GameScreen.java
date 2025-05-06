package Poker.Game;

import Poker.Game.Client.PokerClient;
import Poker.Game.PacketsClasses.*;
import Poker.Game.PacketsClasses.Action;
import Poker.Game.core.Card;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.*;
import java.util.List;

public class GameScreen implements Screen, Poker.Game.ClientListener {
    private final PokerApp app;
    private final PokerClient client;
    private final boolean isHost;
    private TextButton startBtn;
    private List<String> currentPlayers = new ArrayList<>();
    private final float radius = 250f;// Радиус круга для расстановки
    private final float tableCenterX = Gdx.graphics.getWidth() / 2f;
    private final float tableCenterY = Gdx.graphics.getHeight() / 2f;
    private final List<Image> playerAvatars = new ArrayList<>();
    private final List<CardActor> playerCardActors = new ArrayList<>();
    private final Map<String, Integer> nicknameToIndex = new HashMap<>();
    private Map<Integer, Image> playerIdToAvatar = new HashMap<>();
    private Label potLabel;


    private Texture avatarTexture;

    private Stage stage;
    private Skin skin;

    // UI-элементы
    private TextButton foldBtn, callBtn, raiseBtn,checkBtn;
    private Slider raiseSlider;
    private Label raiseAmountLabel;
    private Table chatTable;
    private ScrollPane chatScroll;
    private Table chatMessages;

    private TextField chatInputField;
    private TextButton sendButton;




    // Для текущего запроса хода
    private int currentPlayerId;
    private Action[] currentActions;

    //Для баланса игроков
    private Map<String, Double> playerBalances = new HashMap<String, Double>();
    private Map<String, Label> balanceLabelsById = new HashMap<>();
    private final List<CardActor> tableCardActors = new ArrayList<>();
    Map<Integer, Label> playerBetLabels = new HashMap<>();




    public GameScreen(PokerApp app, PokerClient client, boolean isHost) {
        this.app    = app;
        this.client = client;
        this.isHost = isHost;
        // Регистрируем себя как слушатель сетевых событий
        this.client.setListener(this);
    }

    @Override
    public void show() {
        CardTextureManager.load();

        avatarTexture = new Texture(Gdx.files.internal("sgx/raw/defaulrAvatar.png"));
        stage = new Stage(new ScreenViewport());
        Texture bgTexture = new Texture(Gdx.files.internal("sgx/raw/171.jpg"));
        Image background = new Image(bgTexture);
        background.setFillParent(true); // Растягивает на весь экран
        stage.addActor(background);
        // Добавляем первым, чтобы был позади

        // POKER TABLE
        Texture tableTexture = new Texture(Gdx.files.internal("sgx/raw/Atp.png"), true); // true = генерировать mipmaps
        tableTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);      // сглаживание

        Image tableImage = new Image(new TextureRegionDrawable(new TextureRegion(tableTexture)));
        tableImage.setTouchable(Touchable.disabled); // Не перехватывает клики
        tableImage.setSize(1000,800);
        // Центрируем на экране
        tableImage.setPosition(
            (Gdx.graphics.getWidth()  - tableImage.getWidth()+20)/2f,
            (Gdx.graphics.getHeight() - tableImage.getHeight())/2f
        );

        // Добавляем поверх фона
        stage.addActor(tableImage);







        this.skin = new Skin(Gdx.files.internal("sgx/skin/sgx-ui.json"),
            new TextureAtlas(Gdx.files.internal("sgx/skin/sgx-ui.atlas")));
        Gdx.input.setInputProcessor(stage);










        // === Создаём UI ===
        foldBtn    = new TextButton("Fold",  skin);
        callBtn    = new TextButton("Call",  skin);
        raiseBtn   = new TextButton("Raise", skin);
        checkBtn = new TextButton("Check", skin);
        startBtn = new TextButton("Start Game", skin);
        raiseSlider = new Slider(20, 1000, 10, false, skin); // от 10 до 1000 с шагом 10
        raiseAmountLabel = new Label("20$", skin); // отображение текущей суммы рейза



        //Bank (center) main pot!
        potLabel = new Label("Pot: 0", skin);
        potLabel.setFontScale(1.2f);
        potLabel.setPosition(stage.getWidth() / 2f - potLabel.getWidth() / 2f, stage.getHeight() / 2f + 100); // по центру
        stage.addActor(potLabel);




        //Chat and info label in 1 tool:
        chatMessages = new Table();
        chatMessages.top().left(); // Выравнивание
        chatMessages.defaults().pad(5).left().width(280); // ширина + паддинг

        chatScroll = new ScrollPane(chatMessages, skin);
        chatScroll.setScrollingDisabled(true, false);
        chatScroll.setFadeScrollBars(false);
        chatScroll.setScrollbarsOnTop(false);
        chatScroll.setForceScroll(false, true);
        chatScroll.setSize(300, 200);
        chatScroll.setPosition(10, 40);
        stage.addActor(chatScroll);

        chatInputField = new TextField("", skin);
        chatInputField.setMessageText("Type a message...");
        chatInputField.setPosition(10, 1);
        chatInputField.setSize(300, 40);

        sendButton = new TextButton("Send", skin);
        sendButton.setSize(70, 30);
        sendButton.setPosition(310,40);

        stage.addActor(chatInputField);
        stage.addActor(sendButton);







        // Задаём позиции
        checkBtn.setPosition(870, 10);
        checkBtn.setSize(110, 80);
        foldBtn.setPosition(750, 10);
        foldBtn.setSize(110,80);
        callBtn.setPosition(870, 10);
        callBtn.setSize(110,80);
        raiseSlider.setPosition(990, 100);
        raiseAmountLabel.setPosition(990, 140);
        raiseSlider.setSize(110, 30);
        raiseBtn.setPosition(990, 10);
        raiseBtn.setSize(110,80);
        startBtn.setPosition(500, 20);






        // Изначально прячем кнопки действий
        hideActionUI();

        // Добавляем на сцену
        stage.addActor(startBtn);
        stage.addActor(foldBtn);
        stage.addActor(callBtn);
        stage.addActor(raiseBtn);
        stage.addActor(checkBtn);
        stage.addActor(raiseSlider);
        stage.addActor(raiseAmountLabel);


        // === Логика кнопок ===




        startBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                client.sendGameStart();       // отправляем GameStartRequest
                startBtn.setVisible(false);   // прячем кнопку
            }
        });
        checkBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                client.sendCheck(currentPlayerId); // реализация позже
                hideActionUI();
            }
        });
        sendButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String text = chatInputField.getText().trim();
                if (!text.isEmpty()) {
                    client.sendChatMessage(text);  // Предполагаем, что ты добавишь соответствующий метод
                    chatInputField.setText("");
                }
            }
        });


        foldBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                client.sendFold(currentPlayerId);
                hideActionUI();
            }
        });
        raiseSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int value = (int) raiseSlider.getValue();
                raiseAmountLabel.setText(value + "$");
            }
        });

        callBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                client.sendCall(currentPlayerId);
                hideActionUI();
            }
        });

        raiseBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int amt = (int) raiseSlider.getValue();
                client.sendRaise(currentPlayerId, amt);
                hideActionUI();
                raiseSlider.setValue(20);
            }
        });
        chatInputField.addListener(new InputListener() {
            @Override
            public boolean keyTyped(InputEvent event, char character) {
                if (character == '\n') {
                    sendButton.toggle(); // и/или просто вызвать `sendButton.fire(new ChangeEvent())`
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height); }
    @Override public void pause()  { /* не нужно */ }
    @Override public void resume() { /* не нужно */ }
    @Override public void hide()   { /* не нужно */ }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        if (avatarTexture != null) avatarTexture.dispose();
    }

    // Спрятать кнопки и показать общую надпись
    private void hideActionUI() {
        checkBtn.setVisible(false);
        foldBtn.setVisible(false);
        callBtn.setVisible(false);
        raiseSlider.setVisible(false);
        raiseAmountLabel.setVisible(false);
        raiseBtn.setVisible(false);
        startBtn.setVisible(false); // пока прячем
    }

    // === Реализация ClientListener ===

    @Override
    public void onJoinResponse(JoinResponse resp) {
        Gdx.app.postRunnable(() -> {
            addChatMessage("Connected: " + resp.message);
        });
    }


    @Override
    public void onPlayerListUpdate(List<String> nicknames) {
        this.currentPlayers = nicknames;  // сохраняем

        Gdx.app.postRunnable(() -> {
            addChatMessage("Players: " + String.join(", ", nicknames));
            if (isHost && nicknames.size() >= 2) {
                startBtn.setVisible(true);
                addChatMessage("Ready to start! Players: " + nicknames.size());
            } else {
                startBtn.setVisible(false);
            }
        });
    }


    @Override
    public void onGameStarted(GameStartedNotification note) {
        Gdx.app.postRunnable(() -> {
            addChatMessage("Game started!");
            for (Label label : balanceLabelsById.values()) {
                label.remove();
            }
            balanceLabelsById.clear();
            arrangePlayersOnTable(currentPlayers);
        });
    }


    @Override
    public void onCardInfo(CardInfo info) {
        Gdx.app.postRunnable(() -> {
            addChatMessage("Your cards: " + info.getHand());
            showPlayerCards(info.getHand());
        });
    }


    @Override
    public void onGameStats(GameStats stats) {
        Gdx.app.postRunnable(() ->
            addChatMessage("Stats: " + stats.getStats())
        );
    }

    @Override
    public void onBlinds(BlindsNotification note) {
        Gdx.app.postRunnable(() ->
            addChatMessage(note.getSmallBlind())
        );
    }

    @Override
    public void onActionRequest(ActionRequest req) {
        System.out.println("onActionRequest called for player: " + req.playerId);
        this.currentPlayerId  = req.playerId;
        this.currentActions   = req.availableActions;

        // Показываем кнопки
        Gdx.app.postRunnable(() -> {
            Set<String> availableNames = new HashSet<>();
            for (Action action : currentActions) {
                availableNames.add(action.name);
            }

            System.out.println("Available actions: " + availableNames);

            checkBtn.setVisible(availableNames.contains("check"));
            callBtn.setVisible(availableNames.contains("call"));
            raiseBtn.setVisible(availableNames.contains("raise"));
            foldBtn.setVisible(availableNames.contains("fold"));

            raiseSlider.setVisible(availableNames.contains("raise"));
            raiseAmountLabel.setVisible(availableNames.contains("raise"));
        });
    }


    @Override
    public void onDisconnected() {
        Gdx.app.postRunnable(() ->
            addChatMessage("Disconnected from server!")
        );
    }

    @Override
    public void onPlayerBalanceUpdate(PlayerBalanceUpdate update) {
        String playerName = update.name;
        double balance = update.newBalance;
        playerBalances.put(playerName, balance);

        Label label = balanceLabelsById.get(playerName); // ✅ исправлено
        if (label != null) {
            Gdx.app.postRunnable(() -> label.setText(balance + "$"));
        }
    }

    @Override
    public void onTableCardsInfo(TableCardsInfo tableCardsInfo) {
        Gdx.app.postRunnable(() -> showTableCards(tableCardsInfo.getCards()));
    }


    @Override
    public void onClearBets() {
        Gdx.app.postRunnable(() -> {
            clearPlayerBets();
        });
    }
    @Override
    public void onPotUpdate(PotUpdate update) {
        updatePot(update.getPotAmount());
    }

    @Override
    public void onPlayerBetUpdate(PlayerBetUpdate update) {
        Gdx.app.postRunnable(() -> {
            showPlayerBet(update.playerId, update.amount);
        });
    }

    public void onChatMessage(ChatMessage object) {
        Gdx.app.postRunnable(() -> {
            String name = object.getName();
            String message = object.getMessage();

            if ("sys".equals(name)) {
                addChatMessage(message);
            } else {
                addChatMessage(name + ": " + message); // Белый текст
            }
        });
    }



    private void showTableCards(ArrayList<Card> tableCards) {
        // Удаляем старые карты со стола
        for (CardActor actor : tableCardActors) {
            actor.remove();
        }
        tableCardActors.clear();

        float cardWidth = 90;
        float cardHeight = 134;
        float spacing = 10; // Расстояние между картами
        float totalWidth = tableCards.size() * cardWidth + (tableCards.size() - 1) * spacing;
        float startX = tableCenterX - totalWidth / 2f;
        float y = tableCenterY - cardHeight / 2f + 30; // немного выше центра стола

        for (int i = 0; i < tableCards.size(); i++) {
            Card card = tableCards.get(i);
            CardActor cardActor = new CardActor(card);
            cardActor.setSize(cardWidth, cardHeight);
            cardActor.setPosition(startX + i * (cardWidth + spacing), y);

            stage.addActor(cardActor);
            tableCardActors.add(cardActor);
        }
    }

    private void showPlayerCards(List<Card> hand) {
        for (CardActor actor : playerCardActors) {
            actor.remove();
        }
        playerCardActors.clear();

        float spacing = 40;
        float startX = tableCenterX - 60;
        float y = 135;

        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            CardActor cardActor = new CardActor(card);
            cardActor.setSize(90, 134);
            cardActor.setPosition(startX + i * spacing, y);
            stage.addActor(cardActor);
            playerCardActors.add(cardActor);
        }
    }



    private void addChatMessage(String message) {
        Label.LabelStyle style = new Label.LabelStyle(skin.getFont("font"), Color.WHITE);
        Label label = new Label(message, style);
        label.setWrap(true);
        label.setAlignment(Align.left);
        label.setWidth(280); // важно для переноса
        chatMessages.add(label).left().width(280).padBottom(5).row();
    }


    public void clearPlayerBets() {
        for (Label label : playerBetLabels.values()) {
            label.remove();
        }
        playerBetLabels.clear();
    }


    private void showPlayerBet(int playerId, double amount) {
        // Не показываем ставку, если она равна 0
        if (amount <= 0) {
            Label oldLabel = playerBetLabels.get(playerId);
            if (oldLabel != null) {
                oldLabel.remove();
                playerBetLabels.remove(playerId);
            }
            return;
        }

        String nickname = client.getNicknameById(playerId);
        if (nickname == null) return;

        Integer index = nicknameToIndex.get(nickname);
        if (index == null || index >= playerAvatars.size()) return;

        // Удаляем старый лейбл
        Label oldLabel = playerBetLabels.get(playerId);
        if (oldLabel != null) oldLabel.remove();

        Image avatar = playerAvatars.get(index);
        float x = avatar.getX();
        float y = avatar.getY();

        Label.LabelStyle betStyle = new Label.LabelStyle(skin.getFont("font"), Color.WHITE);
        betStyle.background = skin.newDrawable("white", Color.DARK_GRAY); // фон
        Label betLabel = new Label((int) amount + "$", betStyle);
        betLabel.setFontScale(1.2f);
        betLabel.setColor(Color.GOLD); // цвет текста
        betLabel.setAlignment(Align.center);
        betLabel.setSize(60, 40); // делаем его как фишку
        betLabel.setPosition(x - 50, y + 25);

        stage.addActor(betLabel);
        playerBetLabels.put(playerId, betLabel);
    }


    private void arrangePlayersOnTable(List<String> players) {
        for (Actor actor : playerAvatars) {
            actor.remove();
        }
        playerAvatars.clear();

        int totalPlayers = players.size();
        int myIndex = players.indexOf(client.getNickName());
        if (myIndex == -1) return;

        List<String> ordered = new ArrayList<>();
        for (int i = 0; i < totalPlayers; i++) {
            ordered.add(players.get((myIndex + i) % totalPlayers));
        }


        for (int i = 0; i < totalPlayers; i++) {
            String nickname = ordered.get(i);

            Image avatar = new Image(avatarTexture); // Используем заранее загруженный texture
            avatar.setSize(64, 64);
            avatar.setScaling(Scaling.fit); // помогает сохранить пропорции, если аватарка встроена в Image
            nicknameToIndex.put(nickname, i); // <-- ЭТОГО НЕ ХВАТАЕТ!

            float x, y;
            if (i == 0) {
                x = tableCenterX - 32;
                y = 70;
            } else {
                float angle;
                if (totalPlayers == 2) {
                    angle = (float) (Math.PI / 2); // напротив
                } else if (totalPlayers == 3) {
                    angle = (float) (Math.PI / 2 + (i - 1.5f) * Math.PI / 2);
                } else {
                    angle = (float) (-Math.PI / 2 + 2 * Math.PI * i / totalPlayers);
                }

                x = tableCenterX + (float) Math.cos(angle) * radius - 32;
                y = tableCenterY + (float) Math.sin(angle) * radius + 45;
            }
            avatar.setPosition(x, y);
            stage.addActor(avatar);
            playerAvatars.add(avatar);

            Label nameLabel = new Label(nickname, skin);
            nameLabel.setPosition(x, y - 20);
            stage.addActor(nameLabel);
            Label balanceLabel = new Label("$", skin);
            balanceLabel.setPosition(x, y - 40);
            Double balance = playerBalances.get(nickname);
            if (balance != null) {
                balanceLabel.setText(balance + "$");
            } else {
                balanceLabel.setText("?"); // на случай если не пришло
            }
            stage.addActor(balanceLabel);
            balanceLabelsById.put(nickname, balanceLabel); // ← используем имя каждого игрока!
        }
    }
    public void updatePot(double potValue) {
        Gdx.app.postRunnable(() -> {
            if (potLabel != null) {
                potLabel.setText("Pot: " + potValue + "$");
            }
        });
    }

}

