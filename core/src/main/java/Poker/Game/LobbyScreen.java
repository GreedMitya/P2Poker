package Poker.Game;

import Poker.Game.Client.PokerClient;
import Poker.Game.Server.PokerServer;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.io.IOException;

public class LobbyScreen implements Screen {
    private final PokerApp app;
    private Stage stage;
    private Skin skin;

    public LobbyScreen(PokerApp app) {
        this.app = app;
    }

    @Override
    public void show() {

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);


        // Загружаем готовый скин
        Skin skin = new Skin(Gdx.files.internal("sgx/skin/sgx-ui.json"),
            new TextureAtlas(Gdx.files.internal("sgx/skin/sgx-ui.atlas")));


        // Создаем виджеты
        final TextField nickField = new TextField("", skin);
        nickField.setMessageText("Your name: ");

        final TextField ipField = new TextField("", skin);
        ipField.setMessageText("IP host: ");

        TextButton hostBtn = new TextButton("Host", skin);
        TextButton joinBtn = new TextButton("Connect", skin);

        // Обработчики кнопок
        hostBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                startGame(true, "localhost", nickField.getText().trim());
            }
        });

        joinBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                startGame(false, ipField.getText().trim(), nickField.getText().trim());
            }
        });

        // Загружаем фон (замени путь на актуальный)
        Texture bgTexture = new Texture(Gdx.files.internal("sgx/raw/background.png"));
        Image background = new Image(bgTexture);
        background.setFillParent(true); // Растягивает на весь экран
        stage.addActor(background); // Добавляем первым, чтобы был позади


        // Используем таблицу для расположения
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        float fieldW = 300, fieldH = 40, btnW = 250, btnH = 50, pad = 15;
        table.add(nickField).size(fieldW, fieldH).pad(pad).row();
        table.add(ipField).size(fieldW, fieldH).pad(pad).row();
        table.add(hostBtn).size(btnW, btnH).pad(pad).row();
        table.add(joinBtn).size(btnW, btnH);

        stage.addActor(table);
    }

    private void startGame(boolean isHost, String ip, String nick) {
        if (isHost) {
            new Thread(() -> {
                try {
                    new PokerServer().start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, "PokerServer").start();
        }

        PokerClient client = new PokerClient();
        GameScreen gameScreen = new GameScreen(app, client,isHost);
        app.setScreen(gameScreen);

        new Thread(() -> {
            try {
                client.start(ip, nick);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "PokerClient").start();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1, 1, 1, 1); // белый фон, если ничего не отрисуется
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause()  { }
    @Override public void resume() { }
    @Override public void hide()   { }
    @Override public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
