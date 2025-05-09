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
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.*;
import java.util.List;


public class GameScreen implements Screen, Poker.Game.ClientListener {
    private final PokerApp app;
    private final PokerClient client;
    private final boolean isHost;

    private Stage stage;
    private Skin skin;
    private Texture avatarTexture;

    // Все игроки по Id
    private final Map<Integer, PlayerActor> playerActorsById = new HashMap<>();
    private List<String> currentPlayers = new ArrayList<>();
    private final Map<String, Double> playerBalances = new HashMap<>();

    //Карты игрока
    private final List<CardActor> playerCardActors = new ArrayList<>();


    // Для центральных карт
    private final List<CardActor> tableCardActors = new ArrayList<>();
    private final float radius       = 250f;
    private final float tableCenterX = Gdx.graphics.getWidth()/2f;
    private final float tableCenterY = Gdx.graphics.getHeight()/2f;

    // UI: кнопки и чат
    private TextButton startBtn, foldBtn, callBtn, raiseBtn, checkBtn;
    private Slider raiseSlider;
    private Label raiseAmountLabel, potLabel;
    private Table chatMessages;
    private ScrollPane chatScroll;
    private TextField chatInputField;
    private TextButton sendButton;

    // Текущий запрос хода
    private int currentPlayerId;
    private Action[] currentActions;

    public GameScreen(PokerApp app, PokerClient client, boolean isHost) {
        this.app    = app;
        this.client = client;
        this.isHost = isHost;
        this.client.setListener(this);
    }

    @Override
    public void show() {
        // Загрузка карт
        CardTextureManager.load();

        // Сцена и фон
        avatarTexture = new Texture(Gdx.files.internal("sgx/raw/defaulrAvatar.png"));
        stage = new Stage(new ScreenViewport());
        Image bg = new Image(new Texture(Gdx.files.internal("sgx/raw/171.jpg")));
        bg.setFillParent(true);
        stage.addActor(bg);

        // Стол
        Texture tableTex = new Texture(Gdx.files.internal("sgx/raw/Atp.png"), true);
        tableTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Image table = new Image(new TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(tableTex)));
        table.setSize(1000, 800);
        table.setPosition((Gdx.graphics.getWidth()-1000+20)/2f,
            (Gdx.graphics.getHeight()-800)/2f);
        table.setTouchable(Touchable.disabled);
        stage.addActor(table);

        // Skin и ввод
        skin = new Skin(Gdx.files.internal("sgx/skin/sgx-ui.json"),
            new TextureAtlas(Gdx.files.internal("sgx/skin/sgx-ui.atlas")));
        Gdx.input.setInputProcessor(stage);

        // Пот
        potLabel = new Label("Pot: 0", skin);
        potLabel.setFontScale(1.2f);
        potLabel.setColor(Color.GOLD);
        potLabel.setPosition(stage.getWidth()/2f - potLabel.getWidth()/2f,
            stage.getHeight()/2f + 100);
        stage.addActor(potLabel);

        setupActionButtons();
        setupChat();
    }

    private void setupActionButtons() {
        startBtn = new TextButton("Start Game", skin);
        foldBtn  = new TextButton("Fold", skin);
        callBtn  = new TextButton("Call", skin);
        raiseBtn = new TextButton("Raise", skin);
        checkBtn = new TextButton("Check", skin);
        raiseSlider      = new Slider(20, 1000, 10, false, skin);
        raiseAmountLabel = new Label("20$", skin);

        startBtn.setPosition(500, 20);
        startBtn.setSize(300, 60);
        foldBtn.setPosition(900, 10);
        foldBtn.setSize(110, 60);
        callBtn.setPosition(1020, 10);
        callBtn.setSize(110, 60);
        checkBtn.setPosition(1020, 10);
        checkBtn.setSize(110, 60);
        raiseBtn.setPosition(1140, 10);
        raiseBtn.setSize(110, 60);
        raiseSlider.setPosition(1040, 80);
        raiseSlider.setSize(200,60);
        raiseAmountLabel.setPosition(1040,140);

        startBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                client.sendGameStart();
                startBtn.setVisible(false);
            }
        });
        foldBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                client.sendFold(currentPlayerId);
                hideActionUI();
            }
        });
        callBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                client.sendCall(currentPlayerId);

                hideActionUI();
            }
        });
        checkBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                client.sendCheck(currentPlayerId);
                hideActionUI();
            }
        });
        raiseBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                client.sendRaise(currentPlayerId, (int)raiseSlider.getValue());
                raiseSlider.setValue(20);
                hideActionUI();
            }
        });
        raiseSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                raiseAmountLabel.setText((int)raiseSlider.getValue() + "$");
            }
        });

        hideActionUI();
        stage.addActor(startBtn);
        stage.addActor(foldBtn);
        stage.addActor(callBtn);
        stage.addActor(checkBtn);
        stage.addActor(raiseBtn);
        stage.addActor(raiseSlider);
        stage.addActor(raiseAmountLabel);
    }

    private void hideActionUI() {
        startBtn.setVisible(false);
        foldBtn.setVisible(false);
        callBtn.setVisible(false);
        checkBtn.setVisible(false);
        raiseBtn.setVisible(false);
        raiseSlider.setVisible(false);
        raiseAmountLabel.setVisible(false);
    }
    private void setupChat() {
        chatMessages = new Table();
        chatMessages.top().left();
        chatMessages.defaults().pad(5).left().width(280);

        chatScroll = new ScrollPane(chatMessages, skin);
        chatScroll.setSize(300, 200);
        chatScroll.setPosition(10, 40);
        chatScroll.setScrollingDisabled(true, false);
        stage.addActor(chatScroll);

        chatInputField = new TextField("", skin);
        chatInputField.setMessageText("Type a message...");
        chatInputField.setSize(300, 40);
        chatInputField.setPosition(10, 1);
        stage.addActor(chatInputField);

        sendButton = new TextButton("Send", skin);
        sendButton.setSize(70, 30);
        sendButton.setPosition(310,40);
        sendButton.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                String msg = chatInputField.getText().trim();
                if (!msg.isEmpty()) {
                    client.sendChatMessage(msg);
                    chatInputField.setText("");
                }
            }
        });
        stage.addActor(sendButton);
    }
    @Override
    public void resize(int width, int height) {
        // вот этот параметр `true` — он гарантирует,
        // что камера не будет «резать» сцену,
        // а отцентрирует её и подгонит под новое соотношение сторон
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause()  { /* не нужно */ }

    @Override public void resume() { /* не нужно */ }
    @Override public void hide()   { /* не нужно */ }
    @Override
    public void render(float delta) {
        ScreenUtils.clear(0,0,0,1);
        stage.act(delta);
        stage.draw();
    }

    @Override public void dispose() {
        stage.dispose();
        skin.dispose();
        avatarTexture.dispose();
    }

    // === Реализация ClientListener ===

    @Override
    public void onPlayerListUpdate(List<String> nicknames) {
        this.currentPlayers = nicknames;
        Gdx.app.postRunnable(() -> {
            if (isHost && nicknames.size() >= 2) startBtn.setVisible(true);
        });
    }

    @Override
    public void onJoinResponse(JoinResponse resp) {
        Gdx.app.postRunnable(() -> {
            addChatMessage("Connected: " + resp.message);
        });
    }

    @Override
    public void onGameStarted(GameStartedNotification note) {
        Gdx.app.postRunnable(() -> arrangePlayersOnTable(currentPlayers));
    }


    @Override
    public void onGameStats(GameStats stats) {
        Gdx.app.postRunnable(() ->
            addChatMessage("Stats: " + stats.getStats())
        );
    }

    private void arrangePlayersOnTable(List<String> players) {
        // очищаем
        for (PlayerActor pa : playerActorsById.values()) pa.remove();
        playerActorsById.clear();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        int total   = players.size();
        int myIndex = players.indexOf(client.getNickName());

        for (int i = 0; i < total; i++) {
            String nick = players.get((myIndex + i) % total);
            int id      = client.getIdByNickname(nick);

            PlayerActor pa = new PlayerActor(
                id, nick,
                playerBalances.getOrDefault(nick, 0.0),
                avatarTexture, skin
            );
            // В конструкторе он уже вызвал pack() и setSize(...)

            // Считаем позицию по кругу
            float angle = (float)(-Math.PI/2 + 2*Math.PI*i/total);
            float x = tableCenterX + radius * (float)Math.cos(angle) - pa.getWidth()/2f;
            float y = tableCenterY + radius * (float)Math.sin(angle) - pa.getHeight()/2f;

            pa.setPosition(x, y);
            stage.addActor(pa);
            playerActorsById.put(id, pa);

            if (id != client.getMyId()) {
                pa.showCardBacks();
            }
        }
    }

    @Override
    public void onPlayerBalanceUpdate(PlayerBalanceUpdate upd) {
        playerBalances.put(upd.name, upd.newBalance);
        Gdx.app.postRunnable(() -> {
            PlayerActor pa = playerActorsById.get(client.getIdByNickname(upd.name));
            if (pa != null) pa.updateBalance(upd.newBalance);
        });
    }
    @Override
    public void onPlayerBetUpdate(PlayerBetUpdate upd) {
        Gdx.app.postRunnable(() -> {
            PlayerActor pa = playerActorsById.get(upd.playerId);
            if (pa != null) pa.showBet(upd.amount);
        });
    }



    @Override
    public void onBlinds(BlindsNotification note) {
        Gdx.app.postRunnable(() -> {
            addChatMessage(note.getSmallBlind());
            for (PlayerActor playerActor : playerActorsById.values()) {
                playerActor.showCardBacks(); // Показываем рубашки карт
            }
        });
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
    public void onTableCardsInfo(TableCardsInfo tableCardsInfo) {
        Gdx.app.postRunnable(() -> showTableCards(tableCardsInfo.getCards()));
    }

    @Override
    public void onPotUpdate(PotUpdate update) {
        updatePot(update.getPotAmount());
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

    @Override
    public void onPlayerFold(FoldNotification notif) {
        Gdx.app.postRunnable(() -> {
            PlayerActor pa = playerActorsById.get(notif.playerId);
            if (pa != null) pa.clearCardBacks();
        });
    }

    @Override
    public void onCardInfo(CardInfo info) {
        Gdx.app.postRunnable(() -> {
            addChatMessage("Your cards: " + info.getHand());
            showPlayerCards(info.getHand());
        });
    }

    // Показывает собственные две карты игрока

    private void showPlayerCards(List<Card> hand) {
        for (CardActor actor : playerCardActors) {
            actor.remove();
        }
        playerCardActors.clear();

        // Позиционируем
        float spacing = 40;
        float startX  = tableCenterX - spacing +16;// / 2f; // чтобы карты чуть влево/вправо разошлись
        float y       = 135;                         // нижняя часть экрана

        for (int i = 0; i < hand.size(); i++) {
            CardActor cardActor = new CardActor(hand.get(i));
            cardActor.setSize(90, 134);
            cardActor.setPosition(startX + i * spacing, y);
            stage.addActor(cardActor);
            playerCardActors.add(cardActor);
        }
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

    private void addChatMessage(String message) {
        Label.LabelStyle style = new Label.LabelStyle(skin.getFont("font"), Color.WHITE);
        Label label = new Label(message, style);
        label.setWrap(true);
        label.setAlignment(Align.left);
        label.setWidth(280); // важно для переноса
        chatMessages.add(label).left().width(280).padBottom(5).row();
    }

    public void updatePot(double potValue) {
        Gdx.app.postRunnable(() -> {
            if (potLabel != null) {
                potLabel.setText("Pot: " + potValue + "$");
            }
        });
    }
    private List<Card> flattenCardLists(List<List<Card>> nested) {
        List<Card> flatList = new ArrayList<>();
        for (List<Card> inner : nested) {
            if (inner != null) {
                flatList.addAll(inner);
            }
        }
        return flatList;
    }


    @Override
    public void onWinnerInfo(WinnerInfo info) {
        Gdx.app.postRunnable(() -> {
            showWinnersAnimation(
                info.playerIds,
                flattenCardLists(info.winningCards), // если ты хочешь выделять общие карты
                info.amountWon,
                info.combinationNames
            );
        });
    }


    public void showWinnersAnimation(List<Integer> winnerIds, List<Card> winningCards, double totalPotAmount, List<String> combinationNames) {
        Gdx.app.postRunnable(() -> {
            // 1. Скрываем пот
            potLabel.setVisible(false);

            // 2. Подсветка карт
            highlightWinningCards(winningCards, winnerIds);

            // 3. Делим банк
            double portion = totalPotAmount / winnerIds.size();
            for (Integer id : winnerIds) {
                animatePotToWinner(id, portion);
            }

            // 4. Показать комбинацию для каждого победителя
            for (int i = 0; i < winnerIds.size(); i++) {
                Integer id = winnerIds.get(i);
                String comboName = combinationNames.get(i);
                PlayerActor player = playerActorsById.get(id);
                if (player != null) {
                    showWinningComboText(player, comboName); // Показать комбинацию для каждого победителя
                }
            }

            // 5. Вернуть пот через 2.5 секунды
            stage.addAction(Actions.sequence(
                Actions.delay(2.5f),
                Actions.run(() -> potLabel.setVisible(true))
            ));
        });
    }




    private void highlightWinningCards(List<Card> winningCards, List<Integer> winnerIds) {
        Color glowColor = new Color(1, 1, 0, 0.6f);

        for (CardActor actor : tableCardActors) {
            if (winningCards.contains(actor.getCard())) {
                actor.addAction(Actions.sequence(
                    Actions.color(glowColor, 0.3f),
                    Actions.delay(1f),
                    Actions.color(Color.WHITE, 0.3f)
                ));
            }
        }

        for (int id : winnerIds) {
            PlayerActor player = playerActorsById.get(id);
            if (player != null) {
                player.highlightWinningCards(winningCards); // Вот он — правильный вызов
            }
        }
    }


    private void animatePotToWinner(int winnerId, double potAmount) {
        Label flyingPot = new Label("+" + potAmount + "$", skin);
        flyingPot.setFontScale(1.5f);
        flyingPot.setColor(Color.GOLD);
        flyingPot.setPosition(tableCenterX, tableCenterY);
        stage.addActor(flyingPot);

        PlayerActor winner = playerActorsById.get(winnerId);
        if (winner == null) return;

        float targetX = winner.getX() + winner.getWidth() / 2f;
        float targetY = winner.getY() + winner.getHeight() / 2f;

        flyingPot.addAction(Actions.sequence(
            Actions.moveTo(targetX, targetY, 1f),
            Actions.fadeOut(0.5f),
            Actions.run(flyingPot::remove)
        ));
    }
    private void showWinningComboText(PlayerActor player, String comboName) {
        // Создаем лейбл с именем комбинации
        Label comboLabel = new Label(comboName, skin);
        comboLabel.setColor(Color.YELLOW); // Цвет текста
        comboLabel.setFontScale(1.2f); // Размер шрифта

        // Позиционируем лейбл на экране относительно игрока
        float xPos = player.getX() + player.getWidth() / 2f;
        float yPos = player.getY() + player.getHeight() + 10; // Немного выше аватара

        comboLabel.setPosition(xPos, yPos, Align.center);
        stage.addActor(comboLabel);

        // Добавляем анимацию для текста (движение и исчезновение)
        comboLabel.addAction(Actions.sequence(
            Actions.moveBy(0, 20, 1f), // Двигаем текст вверх
            Actions.fadeOut(1f), // Плавно исчезаем
            Actions.run(comboLabel::remove) // Убираем лейбл после завершения
        ));
    }


}

