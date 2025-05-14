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
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.*;
import java.util.List;


public class GameScreen implements Screen, Poker.Game.ClientListener {
    private static final float WORLD_WIDTH  = 1280f;
    private static final float WORLD_HEIGHT = 720f;
    private float cardWidth, cardHeight;
    private float tableCenterX, tableCenterY, radius;
    // === Ссылки на приложение и клиент ===
    private final PokerApp app;
    private final PokerClient client;
    private final boolean isHost;
    private int myPlayerId;
    private static final float CARD_GAP    = 6f;
    // === Сцена, скин, текстуры ===
    private Stage stage;
    private Skin skin;
    private Texture avatarTexture;
    // === Игроки и их актёры ===
    private final Map<Integer, PlayerActor> playerActorsById = new HashMap<>();
    private List<String> currentPlayers = new ArrayList<>();
    private final Map<String, Double> playerBalances = new HashMap<>();
    // === Карты текущего игрока и оппонентов ===
    private List<Card> myHand = new ArrayList<>();
    private final List<CardActor> playerCardActors = new ArrayList<>();
    private final Map<Integer, List<CardActor>> opponentCardActors = new HashMap<>();
    // === Карты на столе ===
    private final List<CardActor> tableCardActors = new ArrayList<>();
    // === UI: кнопки, слайдер, лейблы, чат ===
    private TextButton startBtn, foldBtn, callBtn, raiseBtn, checkBtn,restartBtn;
    private Slider    raiseSlider;
    private Label     raiseAmountLabel, potLabel;
    private Table     chatMessages;
    private ScrollPane chatScroll;
    private TextField chatInputField;
    private TextButton sendButton;
    // === Для управления потоком действий ===
    private int    currentPlayerId;
    private boolean amI;
    private Action[] currentActions;
    // === Транзитные актёры (летящие поты, метки комбинаций) ===
    private final List<Actor> transientActors = new ArrayList<>();
    public GameScreen(PokerApp app, PokerClient client, boolean isHost) {
        this.app    = app;
        this.client = client;
        this.isHost = isHost;
        this.client.setListener(this);
    }

    public int getMyPlayerId() {
        return myPlayerId;
    }

    public void setMyPlayerId(int myPlayerId) {
        this.myPlayerId = myPlayerId;
    }

    @Override
    public void show() {
        // Загрузка карт
        CardTextureManager.load();

        // Сцена и фон
        avatarTexture = new Texture(Gdx.files.internal("sgx/raw/defaulrAvatar.png"));
        FitViewport viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);
        stage = new Stage(viewport);


        float worldW = viewport.getWorldWidth();
        float worldH = viewport.getWorldHeight();
        UIScale.ui = Math.min(worldW, worldH) / 800f;
        float uiScale = UIScale.ui;
        cardWidth  = UIConfig.CARD_BASE_WIDTH  * UIScale.ui;
        cardHeight = UIConfig.CARD_BASE_HEIGHT * UIScale.ui;
        radius     = Math.min(worldW, worldH) * 0.35f;
        tableCenterX = worldW * 0.5f + worldW * 0.05f;
        tableCenterY = worldH * 0.5f - worldH * 0.02f;

        Image bg = new Image(new Texture(Gdx.files.internal("sgx/raw/171.jpg")));
        bg.setSize(WORLD_WIDTH, WORLD_HEIGHT);
        bg.setPosition(0, 0);
        stage.addActor(bg);

        // Стол
        Texture tableTex = new Texture(Gdx.files.internal("sgx/raw/Atp.png"), true);
        tableTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Image table = getTable(tableTex);
        stage.addActor(table);

        // Skin и ввод
        skin = new Skin(Gdx.files.internal("sgx/skin/sgx-ui.json"),
            new TextureAtlas(Gdx.files.internal("sgx/skin/sgx-ui.atlas")));
        Gdx.input.setInputProcessor(stage);

        // Пот
        potLabel = new Label("Pot: 0", skin);
        float fontScale = Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()) / 800f;
        potLabel.setFontScale(fontScale);
        potLabel.setColor(Color.GOLD);
        potLabel.setPosition(stage.getWidth()/2f - potLabel.getWidth()/2f,
            stage.getHeight()/2f + 100);
        stage.addActor(potLabel);

        setupActionButtons();
        setupChat();
    }

    private Image getTable(Texture tableTex) {
        float worldW = stage.getViewport().getWorldWidth();
        float worldH = stage.getViewport().getWorldHeight();
        Image table = new Image(new TextureRegionDrawable(new TextureRegion(tableTex)));
        table.setSize(worldW * 0.9f, worldH * 0.9f);
        table.setPosition((worldW - table.getWidth())/2f + worldW*0.05f,
            (worldH - table.getHeight())/2f - worldH*0.02f);
        table.setTouchable(Touchable.disabled);
        return table;
    }


    private void setupActionButtons() {
        float worldW = stage.getViewport().getWorldWidth();
        float worldH = stage.getViewport().getWorldHeight();
        float uiScale = Math.min(worldW, worldH) / 800f;

        float buttonW = 110f * uiScale;
        float buttonH =  60f * uiScale;
        float padX    =  10f * uiScale;
        float bottomY =  20f * uiScale;


        // Создание кнопок
        startBtn  = new TextButton("Start Game", skin);
        restartBtn= new TextButton("Restart Game", skin);
        foldBtn   = new TextButton("Fold", skin);
        callBtn   = new TextButton("Call", skin);
        checkBtn  = new TextButton("Check", skin);
        raiseBtn  = new TextButton("Raise", skin);
        raiseSlider      = new Slider(20, 1000, 10, false, skin);
        raiseAmountLabel = new Label("20$", skin);

        // Размеры и позиции
        float buttonWidth  = 110f * uiScale;
        float buttonHeight = 60f * uiScale;
        float paddingX     = 10f * uiScale;


        // Позиции кнопок
        startBtn.setSize(300 * uiScale, buttonHeight);
        startBtn.setPosition(stage.getWidth()/2f - startBtn.getWidth()/2f, bottomY);

        restartBtn.setSize(100 * uiScale, buttonHeight);
        restartBtn.setPosition(paddingX, stage.getHeight() - buttonHeight - paddingX);

        foldBtn.setSize(buttonWidth, buttonHeight);
        foldBtn.setPosition(stage.getWidth() - (buttonWidth + paddingX) * 3, bottomY);

        callBtn.setSize(buttonWidth, buttonHeight);
        callBtn.setPosition(stage.getWidth() - (buttonWidth + paddingX) * 2, bottomY);

        checkBtn.setSize(buttonWidth, buttonHeight);
        checkBtn.setPosition(stage.getWidth() - (buttonWidth + paddingX) * 2, bottomY); // одинаково с callBtn

        raiseBtn.setSize(buttonWidth, buttonHeight);
        raiseBtn.setPosition(stage.getWidth() - (buttonWidth + paddingX), bottomY);

        raiseSlider.setSize(200 * uiScale, 60 * uiScale);
        raiseSlider.setPosition(stage.getWidth() - raiseSlider.getWidth() - paddingX, bottomY + buttonHeight + paddingX);

        raiseAmountLabel.setFontScale(uiScale);
        raiseAmountLabel.setPosition(raiseSlider.getX(), raiseSlider.getY() + raiseSlider.getHeight() + paddingX);
        // listeners…
        startBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                client.sendGameStart();
                startBtn.setVisible(false);
            }
        });
        restartBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                RestartGameRequest req = new RestartGameRequest();
                req.senderId = 1; // или client.getMyId(), если он уже определён как 1
                client.sendRestart(req);
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
        // Добавление на сцену
        hideActionUI();
        stage.addActor(startBtn);
        stage.addActor(restartBtn);
        stage.addActor(foldBtn);
        stage.addActor(callBtn);
        stage.addActor(checkBtn);
        stage.addActor(raiseBtn);
        stage.addActor(raiseSlider);
        stage.addActor(raiseAmountLabel);
    }


    private void hideActionUI() {
        startBtn.setVisible(false);
        restartBtn.setVisible(false);
        foldBtn.setVisible(false);
        callBtn.setVisible(false);
        checkBtn.setVisible(false);
        raiseBtn.setVisible(false);
        raiseSlider.setVisible(false);
        raiseAmountLabel.setVisible(false);
    }
    private void setupChat() {
        float worldW = stage.getViewport().getWorldWidth();
        float worldH = stage.getViewport().getWorldHeight();
        float uiScale = Math.min(worldW, worldH) / 800f; // Базовая шкала для адаптации под экран

        // Таблица сообщений чата
        chatMessages = new Table();
        chatMessages.top().left();
        chatMessages.defaults().pad(1 * uiScale).left().width(380f * uiScale); // Подстраиваем паддинг и ширину под масштаб

        // ScrollPane для прокрутки сообщений
        chatScroll = new ScrollPane(chatMessages, skin);
        chatScroll.setScrollingDisabled(true, false); // Только вертикальная прокрутка
        chatScroll.setSize(400f * uiScale, 200f * uiScale);
        chatScroll.setPosition(10 * uiScale, 50 * uiScale); // Отступы тоже масштабируем
        stage.addActor(chatScroll);

        // Поле ввода текста
        chatInputField = new TextField("", skin);
        chatInputField.setMessageText("Type a message...");
        chatInputField.setSize(320f * uiScale, 40f * uiScale);
        chatInputField.setPosition(10 * uiScale, 10 * uiScale);
        stage.addActor(chatInputField);

        // Кнопка отправки
        sendButton = new TextButton("Send", skin);
        sendButton.setSize(80f * uiScale, 40f * uiScale);
        sendButton.setPosition(chatInputField.getX() + chatInputField.getWidth() + 10 * uiScale, chatInputField.getY());
        sendButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String msg = chatInputField.getText().trim();
                if (!msg.isEmpty()) {
                    client.sendChatMessage(msg);
                    chatInputField.setText("");

                    // Прокрутка в самый низ
                    chatScroll.layout(); // Убедимся, что ScrollPane обновлён
                    chatScroll.setScrollPercentY(1f); // Прокрутка вниз
                }
            }
        });
        stage.addActor(sendButton);
    }

    // === Resize / render / dispose ===
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        float worldW = stage.getViewport().getWorldWidth();
        float worldH = stage.getViewport().getWorldHeight();
        UIScale.ui = Math.min(worldW, worldH) / 800f;
    }
    @Override public void render(float delta){
        ScreenUtils.clear(0,0,0,1);
        stage.act(delta);
        stage.draw();
    }
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}
    @Override public void dispose(){
        stage.dispose();
        skin.dispose();
        avatarTexture.dispose();
    }
    // === Реализация ClientListener ===
    @Override
    public void onPlayerListUpdate(List<String> nicknames) {
        this.currentPlayers = nicknames;
        setMyPlayerId(client.getMyId());
        Gdx.app.postRunnable(() -> {
            if (isHost && nicknames.size() >= 2) startBtn.setVisible(true);restartBtn.setVisible(true);
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
        Gdx.app.postRunnable(() -> arrangePlayersOnTable(currentPlayers));restartBtn.setVisible(true);
    }


    @Override
    public void onGameStats(GameStats stats) {
        Gdx.app.postRunnable(() ->
            addChatMessage("Stats: " + stats.getStats())
        );
    }

    private void arrangePlayersOnTable(List<String> players) {
        // Удаляем старых актёров
        for (PlayerActor pa : playerActorsById.values()) pa.remove();
        playerActorsById.clear();

        float worldW = stage.getViewport().getWorldWidth();
        float worldH = stage.getViewport().getWorldHeight();

        float tableCenterX = worldW * 0.5f; //- worldW * 0.02f; // влево на 2%
        float tableCenterY = worldH * 0.5f - worldH * 0.08f; // ниже на 8%
        float radius       = Math.min(worldW, worldH) * 0.3f;

        int total   = players.size();
        int myIndex = players.indexOf(client.getNickName());

        // Центр стола: можно сместить вправо/вниз через offset
        float offsetX = 0.05f * worldW; // на 5% экрана вправо
        float offsetY = -0.02f * worldH; // на 2% вниз

        for (int i = 0; i < total; i++) {
            String nick = players.get((myIndex + i) % total);
            int id = client.getIdByNickname(nick);
            boolean amI = (id == myPlayerId);

            PlayerActor pa = new PlayerActor(amI, id, nick,
                playerBalances.getOrDefault(nick, 0.0),
                avatarTexture, skin
            );

            // Угол размещения на круге
            float angle = (float) (-Math.PI / 2 + 2 * Math.PI * i / total); // Начинаем сверху и по кругу

            // Координаты центра игрока
            float actorCenterX = tableCenterX + radius * (float) Math.cos(angle);
            float actorCenterY = tableCenterY + radius * (float) Math.sin(angle);

            // Центрируем по X
            float actorX = actorCenterX - pa.getWidth() / 2f;
            float actorY = actorCenterY;

            // Смещение по Y, чтобы не вылезали за экран
            float topCorrection = pa.getHeight() * 0.6f;
            float bottomCorrection = pa.getHeight() * 0.2f;

            if (angle > Math.PI / 2 || angle < -Math.PI / 2) {
                actorY -= topCorrection; // если сверху — опустить
            } else {
                actorY -= bottomCorrection; // если снизу — слегка подвинуть
            }

            pa.setPosition(actorX, actorY);
            stage.addActor(pa);
            playerActorsById.put(id, pa);
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
                if (!playerActor.isLocalPlayer()) {
                    playerActor.showCardBacks();
                }
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
        animateAllBetsToPot();
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
            List<Card> hand = info.getHand();

            // Находим своего игрока
            PlayerActor me = playerActorsById.get(client.getMyId());
            if (me != null) {
                me.clearCardBacks();               // убирать рубашки, если они были
                me.showHandCards(hand);            // показываем карты лицом
            }

            addChatMessage("Your cards: " + hand);
        });
    }



    @Override
    public void onEndOfHandPacket(EndOfHandPacket packet) {
        Gdx.app.postRunnable(() -> {
            Gdx.app.log("GameScreen", "onEndOfHandPacket ▶ " + packet);
            if (packet.getHandsByPlayerId() == null
                || packet.getWinnerIds() == null
                || packet.getWinningsByPlayerId() == null) {
                Gdx.app.error("GameScreen", "EndOfHandPacket содержит null! Прерываем анимацию.");
                return;
            }

            // 1) Показываем карты оппонентов
            revealOpponentCards(packet.getHandsByPlayerId());
            // Тут же показываем всем КомбоТексты!
            List<Integer> ids   = packet.getWinnerIds();
            List<String> names  = packet.getCombinationNames();
            for (int i = 0; i < ids.size(); i++) {
                PlayerActor p = playerActorsById.get(ids.get(i));
                if (p != null) showWinningComboText(p, names.get(i));
            }


            // 2) Собираем единую последовательность действий
            int winnersCount = packet.getWinnerIds().size();
            float postShowDelay = 2f * winnersCount; // ждать столько же секунд, сколько победителей

            SequenceAction fullSeq = Actions.sequence(
                // a) первичная задержка, чтобы игроки увидели карты
                Actions.delay(2.5f),

                // b) скрыть potLabel и запустить последовательную подсветку
                Actions.run(() -> {
                    potLabel.setVisible(false);
                    showWinnersSequentially(
                        packet.getWinnerIds(),
                        packet.getWinningCards(),
                        packet.getCombinationNames(),
                        packet.getWinningsByPlayerId()
                    );
                }),
                // c) подождать, пока все подсветки/анимации пройдут
                Actions.delay(postShowDelay),
                // d) очистка всех временных элементов и возврат UI
                Actions.run(() -> {
                    clearFloatingActors();
                    potLabel.setVisible(true);
                    for (PlayerActor playerActor : playerActorsById.values()) {
                        if (playerActor.isLocalPlayer()) {
                            // отправляем подтверждение
                            System.out.println("GameScreen"+ "Отправляю ClientReadyForNextRound...");
                            client.sendReadyForNextRound(playerActor.getPlayerId(), true);
                        }
                    }
                })
            );
            // 3) Запускаем на stage
            stage.addAction(fullSeq);
        });
    }

    @Override
    public void onGameRestart() {
    }



    private void revealOpponentCards(Map<Integer, List<Card>> handsByPlayerId) {
        for (Map.Entry<Integer, List<Card>> entry : handsByPlayerId.entrySet()) {
            int pid = entry.getKey();
            if (pid == currentPlayerId) continue;
            PlayerActor pa = playerActorsById.get(pid);
            if (pa == null) continue;

            List<Card> hand = entry.getValue();
            List<CardActor> backs = pa.getBackActors();
            // Может оказаться, что рубашек больше/меньше — тогда подстраиваем:
            int n = Math.min(backs.size(), hand.size());

            for (int i = 0; i < n; i++) {
                CardActor back = backs.get(i);
                Card face = hand.get(i);

                // Последовательность: поворот рубашки, подмена текстуры, разворот лицом
                back.addAction(Actions.sequence(
                    Actions.rotateTo(90, 0.2f),
                    Actions.run(() -> {
                        back.setFaceDown(false);      // переключиться на front
                        back.setCard(face);           // обновить текстуру лицевой стороны
                        back.setRotation(90);         // сохраняем угол, чтобы сразу развернуть
                    }),
                    Actions.rotateTo(0, 0.1f)
                ));
            }
        }
    }


    private void resetAllHighlights() {
        for (CardActor ca : tableCardActors) {
            ca.setHighlight(false);
        }
        for (List<CardActor> opp : opponentCardActors.values()) {
            for (CardActor ca : opp) ca.setHighlight(false);
        }
        for (PlayerActor pa : playerActorsById.values()) {
            for (CardActor ca : pa.getCardActors()) ca.setHighlight(false);
        }
    }
    private void showWinnersSequentially(
        List<Integer> winnerIds,
        List<List<Card>> winningCardsByWinner,
        List<String> combinationNames,
        Map<Integer, Double> winningsByPlayerId
    ) {
        resetAllHighlights();
        clearTransientActors();

        SequenceAction seq = new SequenceAction();

        for (int i = 0; i < winnerIds.size(); i++) {
            int pid       = winnerIds.get(i);
            double amount = winningsByPlayerId.getOrDefault(pid, 0.0);
            if (amount <= 0) continue;  // пропускаем

            List<Card> combo   = winningCardsByWinner.get(i);
            String comboName   = combinationNames.get(i);

            // подсветка
            seq.addAction(Actions.run(() -> highlightWinningCardsForPlayer(pid, combo)));
            // анимация пота
            seq.addAction(Actions.run(() -> animatePotToWinner(pid, amount)));
            // текст
            seq.addAction(Actions.run(() -> {
                PlayerActor p = playerActorsById.get(pid);
                if (p != null) showWinningComboText(p, comboName);
            }));
            // пауза 2 секунды
            seq.addAction(Actions.delay(2f));
            // сброс
            seq.addAction(Actions.run(this::resetAllHighlights));
        }

        // финальный сброс
        seq.addAction(Actions.run(() -> {
            clearTransientActors();
            potLabel.setVisible(true);
        }));

        stage.addAction(seq);
    }



    private void highlightWinningCardsForPlayer(int playerId, List<Card> winningCards) {
        Set<Card> winningSet = new HashSet<>(winningCards);

        // Подсветка борда
        for (CardActor ca : tableCardActors) {
            ca.setHighlight(winningSet.contains(ca.getCard()));
        }

        PlayerActor pa = playerActorsById.get(playerId);
        if (pa == null) return;

        List<CardActor> actors;
        if (playerId == currentPlayerId) {
            // свои карты
            actors = pa.getCardActors();
        } else {
            // «ревеленные» карты оппонента
            actors = pa.getBackActors();
        }

        for (CardActor ca : actors) {
            ca.setHighlight(winningSet.contains(ca.getCard()));
        }
    }




    private void animatePotToWinner(int winnerId, double potAmount) {
        if (potAmount == 0) return;
        Label flyingPot = new Label("+" + String.format("%.0f", potAmount) + "$", skin);
        flyingPot.setFontScale(1.5f);
        flyingPot.setColor(Color.GOLD);
        flyingPot.setPosition(tableCenterX, tableCenterY);
        stage.addActor(flyingPot);
        transientActors.add(flyingPot);

        PlayerActor w = playerActorsById.get(winnerId);
        if (w != null) {
            float tx = w.getX() + w.getWidth() / 2f;
            float ty = w.getY() + w.getHeight() / 2f;
            flyingPot.addAction(Actions.sequence(
                Actions.moveTo(tx, ty, 1f),
                Actions.fadeOut(0.5f),
                Actions.run(() -> {
                    flyingPot.remove();
                    transientActors.remove(flyingPot);
                })
            ));
        }
    }

    private void showWinningComboText(PlayerActor player, String comboName) {
        String text = String.format("%s",
            comboName
        );
        Label combo = new Label(text, skin);
        combo.setColor(Color.YELLOW);
        combo.setFontScale(1.4f);
        combo.pack();
        float x = player.getX() + player.getWidth() / 2f - combo.getWidth();
        float y = player.getY() + player.getHeight() + 10;
        combo.setPosition(x, y);
        stage.addActor(combo);
        transientActors.add(combo);

        combo.getColor().a = 0f;
        combo.addAction(Actions.sequence(
            Actions.fadeIn(0.3f),
            Actions.delay(2f),
            Actions.fadeOut(0.5f),
            Actions.run(() -> {
                combo.remove();
                transientActors.remove(combo);
            })
        ));
    }

    private void clearFloatingActors() {
        // Удаляем все transient-акторы
        for (Actor a : transientActors) {
            if (a.hasParent()) a.remove();
        }
        transientActors.clear();

        // Удаляем выложенные карты оппонентов
        for (List<CardActor> list : opponentCardActors.values()) {
            for (CardActor ca : list) {
                if (ca.hasParent()) ca.remove();
            }
        }
        opponentCardActors.clear();

        // Если есть свои карты или карты стола — тоже убираем
        playerCardActors.forEach(Actor::remove);
        playerCardActors.clear();
        tableCardActors.forEach(Actor::remove);
        tableCardActors.clear();
    }
    private void clearTransientActors() {
        for (Actor a : transientActors) {
            a.remove();
        }
        transientActors.clear();
    }
    private void showTableCards(List<Card> tableCards) {
        tableCardActors.forEach(Actor::remove);
        tableCardActors.clear();

        float totalCards = tableCards.size();
        float totalWidth = totalCards * cardWidth + (totalCards - 1) * CARD_GAP * UIScale.ui;
        float startX     = tableCenterX - totalWidth / 2f;
        float yOffset    = cardHeight * 0.3f;
        float y          = tableCenterY - cardHeight/2f + yOffset;

        for (int i = 0; i < tableCards.size(); i++) {
            CardActor ca = new CardActor(tableCards.get(i));
            ca.setSize(cardWidth, cardHeight);
            ca.setPosition(startX + i * (cardWidth + CARD_GAP * UIScale.ui), y);
            ca.getColor().a = 0f;
            ca.addAction(Actions.fadeIn(0.5f));
            stage.addActor(ca);
            tableCardActors.add(ca);
        }
    }

    private void addChatMessage(String text) {
        Label label;

        // Стилизация сообщений: системные — серые, обычные — белые
        if (text.startsWith("sys:")) {
            label = new Label(text.substring(4), skin); // убираем "sys:" префикс
            label.setColor(Color.DARK_GRAY);
        } else {
            label = new Label(text, skin);
            label.setColor(Color.WHITE);
        }

        label.setWrap(true);
        chatMessages.add(label).expandX().fillX();
        chatMessages.row();

        // Обновляем layout и скроллим вниз
        chatMessages.pack();
        chatScroll.layout();
        chatScroll.setScrollPercentY(1f); // Прокрутка в самый низ
    }


    public void updatePot(double potValue) {
        Gdx.app.postRunnable(() -> {
            if (potLabel != null) {
                potLabel.setText("Pot: " + potValue + "$");
            }
        });
    }
    public void animateAllBetsToPot() {
        Vector2 potPos = new Vector2(potLabel.getX(), potLabel.getY());

        for (PlayerActor player : playerActorsById.values()) {
            double amount = player.getCurrentBetAmount();
            if (amount > 0) {
                Label flyingBet = player.createFlyingBetLabel(amount);
                stage.addActor(flyingBet);

                flyingBet.addAction(Actions.sequence(
                    Actions.moveTo(potPos.x, potPos.y, 0.6f, Interpolation.smooth),
                    Actions.fadeOut(0.2f),
                    Actions.run(flyingBet::remove)
                ));

                player.clearBet();
            }
        }
    }


}

