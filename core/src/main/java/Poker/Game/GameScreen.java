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
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.StretchViewport;


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
    private final List<CardActor> playerCardActors = new ArrayList<>();
    private final Map<Integer, List<CardActor>> opponentCardActors = new HashMap<>();
    // === Карты на столе ===
    private final List<CardActor> tableCardActors = new ArrayList<>();
    // === UI: кнопки, слайдер, лейблы, чат ===
    private ControlPanel controlPanel;
    private Label     raiseAmountLabel, potLabel;
    private boolean startButtonShown = false;

    // === Для управления потоком действий ===
    private int    currentPlayerId;
    private Action[] currentActions;
    // === Транзитные актёры (летящие поты, метки комбинаций) ===
    private Map<Integer, Double> betMap = new HashMap<>();
    private final List<Actor> transientActors = new ArrayList<>();
    private Timer.Task hideButtonsTask; // поле класса
    private float uiScale;
    private ChatPanel chatPanel;
    private ActionPanel actionPanel;



    public GameScreen(PokerApp app, PokerClient client, boolean isHost) {
        UIScale.ui = Math.min(WORLD_WIDTH, WORLD_HEIGHT) / 800f;
        this.uiScale = UIScale.ui;  // обязательно сохранить в поле класса
        this.app    = app;
        this.client = client;
        this.isHost = isHost;
        this.client.setListener(this);
        this.skin = new Skin(Gdx.files.internal("sgx/skin/sgx-ui.json"),
            new TextureAtlas(Gdx.files.internal("sgx/skin/sgx-ui.atlas")));
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
        //FitViewport viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);
        StretchViewport viewport = new StretchViewport(WORLD_WIDTH, WORLD_HEIGHT);
        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);
        cardWidth  = UIConfig.CARD_BASE_WIDTH  * UIScale.ui;
        cardHeight = UIConfig.CARD_BASE_HEIGHT * UIScale.ui;
        radius     = Math.min(WORLD_WIDTH, WORLD_HEIGHT) * 0.35f;
        tableCenterX = WORLD_WIDTH * 0.5f + WORLD_WIDTH * 0.05f;
        tableCenterY = WORLD_HEIGHT * 0.5f - WORLD_HEIGHT * 0.02f;
        Image bg = new Image(new Texture(Gdx.files.internal("sgx/raw/171.jpg")));
        bg.setSize(WORLD_WIDTH, WORLD_HEIGHT);
        bg.setPosition(0, 0);
        stage.addActor(bg);
        // Стол
        Texture tableTex = new Texture(Gdx.files.internal("sgx/raw/Atp.png"), true);
        stage.addActor(new TableActor(tableTex, WORLD_WIDTH, WORLD_HEIGHT));
        // Пот
        potLabel = new Label("Pot: 0", skin);
        float fontScale = Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()) / 800f;
        potLabel.setFontScale(fontScale);
        potLabel.setColor(Color.GOLD);
        potLabel.setPosition(stage.getWidth()/2f - potLabel.getWidth()/2f,
            stage.getHeight()/2f + 100);
        stage.addActor(potLabel);

        chatPanel = new ChatPanel(skin, client, uiScale);
        chatPanel.setPosition(2f * uiScale, 2f * uiScale);
        chatPanel.pack();
        stage.addActor(chatPanel);



        controlPanel = new ControlPanel(skin, uiScale, new ControlPanel.ControlListener() {
            @Override public void onStart() {
                client.sendGameStart();
                controlPanel.startBtn.setVisible(false);
            }

            @Override public void onRestart() {
                client.sendRestart();
            }

            @Override
            public void onDisconnect() {
                client.disconnect();
            }
        });

        controlPanel.setPosition((5f)  * uiScale, (WORLD_HEIGHT-25f) * uiScale); // например, левый нижний угол с отступом
        stage.addActor(controlPanel);
        actionPanel = new ActionPanel(skin, new ActionPanel.ActionListener() {
            @Override
            public void onFold() {
                client.sendFold(myPlayerId);
                PlayerActor me = playerActorsById.get(myPlayerId);
                if (me != null) {
                    me.dimCards(); // затемняем карты
                }
            }


            @Override public void onCall() {
                client.sendCall(myPlayerId);
            }

            @Override public void onCheck() {
                client.sendCheck(myPlayerId);
            }

            @Override public void onRaise(int amount) {
                client.sendRaise(myPlayerId, amount);
            }
        }, uiScale);
        actionPanel.setPosition(
            (WORLD_WIDTH - actionPanel.getWidth()),
            20f * uiScale
        );
        stage.addActor(actionPanel);



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
        chatPanel.updateLayoutIfNeeded();
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
            arrangePlayersOnTable(currentPlayers);

            // Показываем кнопки Хоста лишь один раз,
            // когда игроков стало 2 или больше
            if (isHost
                && nicknames.size() >= 2
                && controlPanel != null
                && !startButtonShown) {
                controlPanel.startBtn.setVisible(true);
                startButtonShown = true;
            }
        });
    }


    @Override
    public void onPlayerOrderPacket(PlayerOrderPacket packet) {
        this.currentPlayers = packet.getLogicalOrder();

        // Запускаем на рендер-потоке
        Gdx.app.postRunnable(() -> {
            // 1. Удаляем всех старых акторов за столом
            for (PlayerActor pa : playerActorsById.values()) {
                pa.remove();
            }
            playerActorsById.clear();
            // 2. Класс arrangePlayersOnTable уже умеет добавлять по списку currentPlayers
            arrangePlayersOnTable(currentPlayers);

            // 3. Сбрасываем UI прошлой раздачи (кнопки, карты)
            actionPanel.hide();
            for (PlayerActor pa : playerActorsById.values()) {
                pa.clearHandCards();
                pa.clearCardBacks();
            }
        });
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
            if (pa != null) {
                pa.showBet(upd.amount);
                if (upd.amount>0){
                    SoundManager.getInstance().play("bet", 1f);
                }
            } else {
                Logger.Game("⚠️ [onPlayerBetUpdate] PlayerActor not found for id=" + upd.playerId + ". Возможно, игрок ещё не добавлен.");
            }
        });
    }
    @Override
    public void onBlinds(BlindsNotification note) {
        Gdx.app.postRunnable(() -> {
            // Каждое сообщение отдельно в чат
            for (String msg : note.getMessages()) {
                chatPanel.addMessage(msg);
            }

            potLabel.setVisible(true);

            for (PlayerActor actor : playerActorsById.values()) {
                if (!actor.isLocalPlayer()) {
                    actor.showCardBacks();
                }

                actor.setDealer(actor.getPlayerId() == note.getDealerId());
            }
        });
    }


    @Override
    public void onActionRequest(ActionRequest req) {
        System.out.println("onActionRequest called for player: " + req.playerId);
        this.currentPlayerId  = req.playerId;
        this.currentActions   = req.availableActions;

        // Отменяем предыдущий таймер скрытия
        if (hideButtonsTask != null) {
            hideButtonsTask.cancel();
            hideButtonsTask = null;
        }

        // Показываем панель действий только если это ты
        if (req.playerId == myPlayerId) {
            Gdx.app.postRunnable(() -> {
                actionPanel.show();

                // Обновим доступные кнопки:
                updateActionButtons(req.availableActions);
            });
        } else {
            actionPanel.hide(); // прячем у других
        }
        hideButtonsTask = Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                Gdx.app.postRunnable(() -> actionPanel.hide());
            }
        }, 30); // через 30 секунд

    }

    private void updateActionButtons(Action[] availableActions) {
        boolean canFold = false, canCall = false, canCheck = false, canRaise = false;
        int minRaise = 20;
        int maxRaise = 1000;

        for (Action a : availableActions) {
            switch (a.name.toLowerCase()) {
                case "fold":
                    canFold = true;
                    break;
                case "call":
                    canCall = true;
                    break;
                case "check":
                    canCheck = true;
                    break;
                case "raise":
                    canRaise = true;
                    // Сохраняем min/max из объекта Action
                    minRaise = (int) Math.round(a.min);
                    maxRaise = (int) Math.round(a.max);
                    break;
            }
        }

        final boolean fCanFold = canFold;
        final boolean fCanCall = canCall;
        final boolean fCanCheck = canCheck;
        final boolean fCanRaise = canRaise;
        final int fMinRaise = minRaise;
        final int fMaxRaise = maxRaise;

        Gdx.app.postRunnable(() -> actionPanel.updateButtons(
            fCanFold, fCanCheck, fCanCall, fCanRaise, fMinRaise, fMaxRaise
        ));
    }




    @Override
    public void onDisconnected() {
        Gdx.app.postRunnable(() ->
            chatPanel.addMessage("Disconnected from server!")
        );
    }
    @Override
    public void onTableCardsInfo(TableCardsInfo tableCardsInfo) {
        Gdx.app.postRunnable(() -> showTableCards(tableCardsInfo.getCards()));
        if (tableCardsInfo.getCards().size()==3) {
            SoundManager.getInstance().play("flipontable", 1f);
            SoundManager.getInstance().play("flipontable", 1f);
            SoundManager.getInstance().play("flipontable", 1f);
        }
        else {
            SoundManager.getInstance().play("flipontable", 1f);
        }
    }
    @Override
    public void onPotUpdate(PotUpdate update) {
        animateAllBetsToPot(betMap);
        updatePot(update.getPotAmount());
    }
    @Override
    public void onGameRestart() {
    }


    @Override
    public void onBetUpdatePack(BetUpdatePack object) {
        betMap = object.getBets();
    }
    @Override
    public void onPlayerFold(FoldNotification notif) {
        Gdx.app.postRunnable(() -> {
            PlayerActor pa = playerActorsById.get(notif.playerId);
            if (pa != null) pa.clearCardBacks();
        });
        SoundManager.getInstance().play("fold", 1f);
    }
    @Override
    public void onCheckPacket() {
        SoundManager.getInstance().play("check", 1f);
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

            chatPanel.addMessage("Your cards: " + hand);
        });
        SoundManager.getInstance().play("flipcard", 1f);
    }

    private void arrangePlayersOnTable(List<String> logicalOrder) {
        // Удаляем старые
        for (PlayerActor pa : playerActorsById.values()) pa.remove();
        playerActorsById.clear();

        float worldW = stage.getViewport().getWorldWidth();
        float worldH = stage.getViewport().getWorldHeight();

        float centerX = worldW / 2f;
        float centerY = worldH / 2f - worldH * 0.08f;

        float a = worldW * 0.35f; // полуось по X (ширина стола)
        float b = worldH * 0.30f; // полуось по Y (высота стола)

        int total = logicalOrder.size();
        int myIndex = logicalOrder.indexOf(client.getNickName());

        // Смещение "я" в visualOrder — только если 6 игроков, тогда я сижу справа внизу
        int visualOffset = (total == 6) ? -1 : 0;
        int shiftedIndex = (myIndex + visualOffset + total) % total;

        // Сдвигаем список так, чтобы "я" оказался в нужной позиции
        List<String> visualOrder = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            visualOrder.add(logicalOrder.get((shiftedIndex + i) % total));
        }


        // Список кол-ва игроков по сторонам: низ, право, верх, лево
        int[] sectorCounts = new int[4];
        if (total == 2) {
            sectorCounts[0] = 1; sectorCounts[1] = 0; sectorCounts[2] = 1; sectorCounts[3] = 0;
        } else if (total == 3) {
            sectorCounts[0] = 1; sectorCounts[1] = 1; sectorCounts[2] = 1; sectorCounts[3] = 0;
        } else if (total == 4) {
            sectorCounts[0] = 1; sectorCounts[1] = 1; sectorCounts[2] = 1; sectorCounts[3] = 1;
        } else if (total == 5) {
            sectorCounts[0] = 1; sectorCounts[1] = 1; sectorCounts[2] = 2; sectorCounts[3] = 1;
        } else if (total == 6) {
            sectorCounts[0] = 2; sectorCounts[1] = 1; sectorCounts[2] = 2; sectorCounts[3] = 1;
        } else {
            sectorCounts[0] = 2; sectorCounts[1] = 1; sectorCounts[2] = 2; sectorCounts[3] = 1;
        }

        int index = 0;
        for (int side = 0; side < 4; side++) {
            int count = sectorCounts[side];
            for (int i = 0; i < count; i++) {
                if (index >= visualOrder.size()) break;

                String nick = visualOrder.get(index++);
                int id = client.getIdByNickname(nick);
                boolean amI = (id == myPlayerId);

                PlayerActor pa = new PlayerActor(amI, id, nick,
                    playerBalances.containsKey(nick) ? playerBalances.get(nick) : 0.0,
                    avatarTexture, skin
                );

                float t = (i + 1f) / (count + 1f);
                float x = centerX;
                float y = centerY;

                if (side == 0) { // низ
                    x = centerX - a + 2 * a * t;
                    y = centerY - b;
                } else if (side == 1) { // право
                    x = centerX + a;
                    y = centerY - b + 2 * b * t;
                } else if (side == 2) { // верх
                    x = centerX + a - 2 * a * t;
                    y = centerY + b;
                } else if (side == 3) { // лево
                    x = centerX - a;
                    y = centerY + b - 2 * b * t;
                }

                x -= pa.getWidth() / 2f;
                y -= pa.getHeight() * (side == 2 ? 0.6f : 0.2f);

                pa.setTableSide(side); // Добавь сюда
                pa.setPosition(x, y);
                stage.addActor(pa);
                playerActorsById.put(id, pa);
            }
        }
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


            // 2) Собираем единую последовательность действийv
            int winnersCount = packet.getWinnerIds().size();
            float postShowDelay = 1f * winnersCount; // ждать столько же секунд, сколько победителей

            SequenceAction fullSeq = Actions.sequence(
                // a) первичная задержка, чтобы игроки увидели карты
                Actions.delay(2f),

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
                    //potLabel.setVisible(true); // Хочу чтобы он появлялся только на начале следующего раунда
                    for (PlayerActor playerActor : playerActorsById.values()) {
                        if (playerActor.isLocalPlayer()) {
                            // отправляем подтверждение
                            System.out.println("GameScreen"+ "ClientReadyForNextRound");
                            client.sendReadyForNextRound(playerActor.getPlayerId(), true);
                        }
                    }
                })
            );
            // 3) Запускаем на stage
            stage.addAction(fullSeq);
        });
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

                back.addAction(Actions.sequence(
                    // 1) Сжимаем по горизонтали до 0 за 0.2 с (имитация правой половины переворота)
                    Actions.scaleTo(0f, 1f, 0.2f),
                    // 2) Меняем сторону карты
                    Actions.run(() -> {
                        back.setFaceDown(false);
                        back.setCard(face);
                    }),
                    // 3) Расширяем назад до полного размера за 0.2 с (левая половина переворота)
                    Actions.scaleTo(1f, 1f, 0.3f)
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
            seq.addAction(Actions.delay(2.5f));
            // сброс
            seq.addAction(Actions.run(this::resetAllHighlights));
        }

        // финальный сброс
        seq.addAction(Actions.run(() -> {
            clearTransientActors();
            // potLabel.setVisible(true); // Хочу чтобы появлялся аж в начале раунда!
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
        SoundManager.getInstance().play("totalpot1", 0.5f);
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



    public void updatePot(double potValue) {
        Gdx.app.postRunnable(() -> {
            if (potLabel != null) {
                potLabel.setText("Pot: " + potValue + "$");
                if (potValue>30) {
                    SoundManager.getInstance().play("totalpot1", 1f);
                }
            }
        });
    }
    public void animateAllBetsToPot(Map<Integer, Double> betMap) {
        Vector2 potPos = new Vector2(potLabel.getX(), potLabel.getY());

        for (Map.Entry<Integer, Double> entry : betMap.entrySet()) {
            PlayerActor player = playerActorsById.get(entry.getKey());
            if (player == null) continue;

            double amount = entry.getValue();
            if (amount <= 0) continue;

            Label flyingBet = player.createFlyingBetLabel(amount);
            stage.addActor(flyingBet);

            flyingBet.addAction(Actions.sequence(
                Actions.moveTo(potPos.x, potPos.y, 0.5f, Interpolation.smooth),
                Actions.fadeOut(0.2f),
                Actions.run(() -> {
                    flyingBet.remove();
                    player.clearBet();
                })
            ));
        }
    }


}

