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
    // === Ссылки на приложение и клиент ===
    private final PokerApp app;
    private final PokerClient client;
    private final boolean isHost;
    // === Константы для карт ===
    private static final float CARD_WIDTH  = 90f;
    private static final float CARD_HEIGHT = 134f;
    private static final float CARD_GAP    = 50f;
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
    private final float radius       = 250f;
    private final float tableCenterX = Gdx.graphics.getWidth()  / 2f;
    private final float tableCenterY = Gdx.graphics.getHeight() / 2f;
    // === UI: кнопки, слайдер, лейблы, чат ===
    private TextButton startBtn, foldBtn, callBtn, raiseBtn, checkBtn;
    private Slider    raiseSlider;
    private Label     raiseAmountLabel, potLabel;
    private Table     chatMessages;
    private ScrollPane chatScroll;
    private TextField chatInputField;
    private TextButton sendButton;
    // === Для управления потоком действий ===
    private int    currentPlayerId;
    private Action[] currentActions;
    // === Транзитные актёры (летящие поты, метки комбинаций) ===
    private final List<Actor> transientActors = new ArrayList<>();

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
        // позиционирование…
        startBtn.setPosition(500, 20); startBtn.setSize(300, 60);
        foldBtn .setPosition(900, 10); foldBtn .setSize(110, 60);
        callBtn .setPosition(1020,10); callBtn .setSize(110, 60);
        checkBtn.setPosition(1020,10); checkBtn.setSize(110, 60);
        raiseBtn.setPosition(1140,10); raiseBtn.setSize(110, 60);
        raiseSlider.setPosition(1040,80); raiseSlider.setSize(200,60);
        raiseAmountLabel.setPosition(1040,140);
        // listeners…
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
    // === Resize / render / dispose ===
    @Override public void resize(int w,int h){ stage.getViewport().update(w,h,true); }
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

    @Override
    public void onEndOfHandPacket(EndOfHandPacket packet) {
        Gdx.app.postRunnable(() -> {
            Gdx.app.log("GameScreen", "onEndOfHandPacket ▶ " + packet);

            if (packet.getHandsByPlayerId() == null || packet.getWinnerIds() == null) {
                Gdx.app.error("GameScreen", "EndOfHandPacket содержит null! Прерываем анимацию.");
                return;
            }

            // 1) Чистим старое
            clearFloatingActors();
            for (List<CardActor> list : opponentCardActors.values()) {
                for (CardActor ca : list) ca.remove();
            }
            opponentCardActors.clear();

            // 2) Показываем карты игроков
            revealOpponentCards(packet.getHandsByPlayerId());
            // 3) Ждём → показываем победителей
            stage.addAction(Actions.sequence(
                Actions.delay(2.5f),
                Actions.run(() -> {
                    potLabel.setVisible(false);
                    showWinnersAnimation(
                        packet.getWinnerIds(),
                        packet.getWinningCards(),
                        packet.getAmountWon(),
                        packet.getCombinationNames()
                    );
                    stage.addAction(Actions.sequence(
                        Actions.delay(2.5f),
                        Actions.run(() -> potLabel.setVisible(true))
                    ));
                })
            ));
        });
    }

    private void revealOpponentCards(Map<Integer, List<Card>> handsByPlayerId) {
        for (Map.Entry<Integer, List<Card>> entry : handsByPlayerId.entrySet()) {
            int pid = entry.getKey();
            List<Card> hand = entry.getValue();
            PlayerActor pa = playerActorsById.get(pid);
            if (pa == null) continue;

            float startX = pa.getX() + pa.getWidth() / 2f - hand.size() * CARD_GAP / 2f;
            float y      = pa.getY() - CARD_HEIGHT - 20;

            List<CardActor> actors = new ArrayList<CardActor>();
            for (int i = 0; i < hand.size(); i++) {
                CardActor ca = new CardActor(hand.get(i));
                ca.setSize(CARD_WIDTH, CARD_HEIGHT);
                ca.setPosition(startX + i * CARD_GAP, y);
                ca.showBack();
                stage.addActor(ca);
                actors.add(ca);

                // flip-анимация
                final int index = i;
                ca.addAction(Actions.sequence(
                    Actions.delay(0.2f * index),
                    Actions.rotateTo(90, 0.2f),
                    Actions.run(new Runnable() {
                        @Override
                        public void run() {
                            ca.showFront();
                        }
                    }),
                    Actions.rotateTo(0, 0.2f)
                ));
            }
            opponentCardActors.put(pid, actors);
        }
    }
    public void showWinnersAnimation(
        List<Integer> winnerIds,
        List<List<Card>> winningCardsByWinner,
        double totalPotAmount,
        List<String> combinationNames
    ) {
        if (winnerIds.isEmpty()) return;

        double portion = totalPotAmount / winnerIds.size();

        for (int i = 0; i < winnerIds.size(); i++) {
            int id = winnerIds.get(i);
            List<Card> cards = winningCardsByWinner.get(i);
            String combo = combinationNames.get(i);

            highlightWinningCardsForPlayer(id, cards);
            animatePotToWinner(id, portion);

            PlayerActor p = playerActorsById.get(id);
            if (p != null) {
                showWinningComboText(p, combo);
            }
        }
    }

    private void highlightWinningCardsForPlayer(int playerId, List<Card> winningCards) {
        Color glow = new Color(1, 1, 0, 0.6f);

        // Подсвечиваем общие карты на столе
        for (CardActor ca : tableCardActors) {
            if (winningCards.contains(ca.getCard())) {
                ca.addAction(Actions.sequence(
                    Actions.color(glow, 0.3f),
                    Actions.delay(1f),
                    Actions.color(Color.WHITE, 0.3f)
                ));
            }
        }

        // Подсветка карт игрока
        PlayerActor p = playerActorsById.get(playerId);
        if (p != null) {
            p.highlightWinningCards(winningCards);
        }
    }

    private void animatePotToWinner(int winnerId, double potAmount) {
        Label flyingPot = new Label("+"+potAmount+"$", skin);
        flyingPot.setFontScale(1.5f);
        flyingPot.setColor(Color.GOLD);
        flyingPot.setPosition(tableCenterX, tableCenterY);
        stage.addActor(flyingPot);
        transientActors.add(flyingPot);

        PlayerActor w = playerActorsById.get(winnerId);
        if (w!=null) {
            float tx = w.getX()+w.getWidth()/2f;
            float ty = w.getY()+w.getHeight()/2f;
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
        Label combo = new Label(comboName, skin);
        combo.setColor(Color.YELLOW);
        combo.setFontScale(1.2f);
        combo.pack();
        float x = player.getX() + player.getWidth()/2f - combo.getWidth()/2f;
        float y = player.getY() + player.getHeight() + 10;
        combo.setPosition(x,y);
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
        for (Actor a : transientActors) {
            if (a != null && a.hasParent()) {
                a.remove();
            }
        }
        transientActors.clear();

        // Дополнительно подчистим акторов в opponentCardActors
        for (List<CardActor> list : opponentCardActors.values()) {
            for (CardActor ca : list) {
                if (ca != null && ca.hasParent()) {
                    ca.remove();
                }
            }
        }
        opponentCardActors.clear();

        // И если ты хочешь гарантировать очистку всех карт игроков:
        playerCardActors.forEach(Actor::remove);
        playerCardActors.clear();

        tableCardActors.forEach(Actor::remove);
        tableCardActors.clear();
    }

    private void showPlayerCards(List<Card> hand) {
        playerCardActors.forEach(Actor::remove);
        playerCardActors.clear();
        float spacing = 40, startX = tableCenterX - spacing +16, y = 135;
        for (int i=0;i<hand.size();i++) {
            CardActor ca = new CardActor(hand.get(i));
            ca.setSize(CARD_WIDTH,CARD_HEIGHT);
            ca.setPosition(startX + i*spacing, y);
            stage.addActor(ca);
            playerCardActors.add(ca);
        }
    }
    private void showTableCards(List<Card> tableCards) {
        tableCardActors.forEach(Actor::remove);
        tableCardActors.clear();
        float spacing=10, totalW=tableCards.size()*CARD_WIDTH+(tableCards.size()-1)*spacing;
        float startX=tableCenterX-totalW/2f, y=tableCenterY-CARD_HEIGHT/2f+30;
        for (int i=0;i<tableCards.size();i++){
            CardActor ca = new CardActor(tableCards.get(i));
            ca.setSize(CARD_WIDTH,CARD_HEIGHT);
            ca.setPosition(startX + i*(CARD_WIDTH+spacing), y);
            stage.addActor(ca);
            tableCardActors.add(ca);
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
        List<Card> out = new ArrayList<Card>();
        for (List<Card> inner : nested) {
            if (inner != null) {
                out.addAll(inner);
            }
        }
        return out;
    }
}

