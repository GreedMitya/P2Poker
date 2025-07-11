package GreedMitya.P2Poker;

import GreedMitya.P2Poker.Client.PokerClient;
import GreedMitya.P2Poker.Client.ServerDiscoverer;
import GreedMitya.P2Poker.Server.PokerServer;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import java.io.IOException;
import static GreedMitya.P2Poker.Server.PokerServer.getLocalIpAddress;


public class LobbyScreen implements Screen {
    private float uiScale = UIScale.ui;
    private final PokerApp app;
    private Stage stage;
    private Skin skin;

    private void showDialog(String message) {
        Dialog dialog = new Dialog("Error", skin);
        dialog.text(message);
        dialog.button("OK");
        dialog.setScale(2f);
        dialog.show(stage);
    }

    private String errorMessage = null;

    public LobbyScreen(PokerApp app) {
        this.app = app;
    }

    public LobbyScreen(PokerApp app, String errorMessage) {
        this.app = app;
        this.errorMessage = errorMessage;
    }


    @Override
    public void show() {
        if (errorMessage != null) {
            Gdx.app.postRunnable(() -> showDialog(errorMessage));
        }
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(
            Gdx.files.internal("sgx/skin/sgx-ui.json"),
            new TextureAtlas(Gdx.files.internal("sgx/skin/sgx-ui.atlas"))
        );

        TextField.TextFieldStyle style = new TextField.TextFieldStyle(skin.get(TextField.TextFieldStyle.class));
        style.font.getData().setScale(2.5f);
        style.font.setColor(Color.BLACK);
        final TextField nickField = new TextField("", style);
        nickField.setMessageText("Your name: ");
        nickField.setTextFieldListener((field, key) -> {
            if (key == '\n' || key == '\r') {
                field.getOnscreenKeyboard().show(false);
                stage.setKeyboardFocus(null);
            }
        });
        nickField.setTextFieldFilter(new TextField.TextFieldFilter() {
            @Override
            public boolean acceptChar(TextField textField, char c) {
                return Character.isLetterOrDigit(c) || c == '_';
            }
        });

        final TextField ipField = new TextField("", style);
        ipField.setMessageText("IP host: ");
        ipField.setTextFieldListener((field, key) -> {
            if (key == '\n' || key == '\r') {
                field.getOnscreenKeyboard().show(false);
                stage.setKeyboardFocus(null);
            }
        });
        TextButton hostBtn = new TextButton("Host", skin);
        hostBtn.getLabel().setFontScale(1.85f);
        TextButton joinBtn = new TextButton("Connect", skin);
        joinBtn.getLabel().setFontScale(1.85f);
        final SelectBox<String> serverList = new SelectBox<>(skin);
        TextButton scanButton = new TextButton("Scan LAN", skin);
        scanButton.getLabel().setFontScale(1.85f);
        scanButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                scanButton.setText("Scanning...");
                scanButton.setDisabled(true);
                new Thread(() -> {
                    final java.util.List<String> servers = ServerDiscoverer.discoverServers(2000);
                    Gdx.app.postRunnable(() -> {
                        scanButton.setText("Scan LAN");
                        scanButton.setDisabled(false);

                        if (servers.isEmpty()) {
                            showDialog("No servers found on LAN");
                        } else {
                            List.ListStyle listStyle = new List.ListStyle();
                            BitmapFont font = new BitmapFont();
                            font.getData().setScale(2f);
                            listStyle.font = font;
                            listStyle.selection = skin.getDrawable("list-selection");
                            final List<String> list = new List<>(listStyle);
                            list.setItems(servers.toArray(new String[0]));
                            ScrollPane pane = new ScrollPane(list, skin);
                            pane.setFadeScrollBars(false);
                            pane.setScrollingDisabled(true, false);
                            pane.setForceScroll(false, true);
                            pane.setOverscroll(false, false);
                            pane.setScrollbarsOnTop(true);
                            pane.setSmoothScrolling(true);

                            Dialog dlg = new Dialog("Select server", skin) {
                                @Override
                                protected void result(Object result) {
                                    if (Boolean.TRUE.equals(result)) {
                                        String ip = list.getSelected();
                                        String nick = nickField.getText().trim();
                                        if (isNickValid(nick)) {
                                            startGame(false, ip, nick);
                                        } else {
                                            showDialog("Invalid nickname!");
                                        }
                                    }
                                }
                            };

                            dlg.getContentTable().add(pane)
                                .width(1000 * uiScale)
                                .height(550 * uiScale)
                                .pad(10)
                                .row();

                            dlg.button("Join", true);
                            dlg.button("Cancel", false);
                            dlg.getButtonTable().top();
                            dlg.getButtonTable().padBottom(20f);
                            dlg.key(com.badlogic.gdx.Input.Keys.ENTER, true);
                            dlg.key(com.badlogic.gdx.Input.Keys.ESCAPE, false);
                            dlg.show(stage);
                            for (Actor a : dlg.getButtonTable().getChildren()) {
                                if (a instanceof TextButton) {
                                    ((TextButton) a).getLabel().setFontScale(1.5f);
                                }
                            }
                        }
                    });
                }).start();
            }
        });
        TextButton exitBtn = new TextButton("Exit", skin);
        exitBtn.getLabel().setFontScale(1.85f);
        exitBtn.getLabel().setColor(Color.WHITE);
        exitBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (Gdx.app.getType() == Application.ApplicationType.Android) {
                    app.getAndroidBridge().exitApp();
                } else {
                    Gdx.app.exit();
                }
            }
        });
        serverList.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String selectedIP = serverList.getSelected();
                if (!selectedIP.equals("No servers found")) {
                    ipField.setText(selectedIP);
                }
            }
        });

        hostBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String nick = nickField.getText().trim();
                if (isNickValid(nick)) {
                    String localIp = getLocalIpAddress();
                    startGame(true, localIp, nick);
                } else {
                    showDialog("Invalid nickname. Use letters, numbers or _ only.");
                }
            }
        });
        joinBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String nick = nickField.getText().trim();
                if (isNickValid(nick)) {
                    String ip = ipField.getText().trim();
                    startGame(false, ip, nick);
                } else {
                    showDialog("Invalid nickname. Use letters, numbers or _ only.");
                }
            }
        });
        Texture bgTexture = new Texture(Gdx.files.internal("sgx/raw/background2.jpg"));
        Image background = new Image(bgTexture);
        background.setFillParent(true);
        stage.addActor(background);

        Table table = new Table();
        table.setFillParent(true);
        table.center();

        float pad = 20f;

        table.add(nickField)
            .width(Value.percentWidth(0.6f, table))
            .height(Value.percentHeight(0.1f, table))
            .pad(pad).row();

        table.add(ipField)
            .width(Value.percentWidth(0.6f, table))
            .height(Value.percentHeight(0.1f, table))
            .pad(pad).row();

        table.add(hostBtn)
            .width(Value.percentWidth(0.5f, table))
            .height(Value.percentHeight(0.09f, table))
            .pad(pad).row();

        table.add(scanButton)
            .width(Value.percentWidth(0.5f, table))
            .height(Value.percentHeight(0.09f, table))
            .pad(pad).row();

        table.add(joinBtn)
            .width(Value.percentWidth(0.5f, table))
            .height(Value.percentHeight(0.09f, table))
            .pad(pad).row();

        table.add(exitBtn)
            .width(Value.percentWidth(0.5f, table))
            .height(Value.percentHeight(0.09f, table))
            .pad(pad).row();


        hostBtn.getLabel().setColor(Color.WHITE);
        joinBtn.getLabel().setColor(Color.WHITE);

        stage.addActor(table);
    }

    private boolean isNickValid(String nick) {
        return nick != null && !nick.isEmpty() && nick.matches("[A-Za-z0-9_]{1,16}");
    }

    private void startGame(boolean isHost, String ip, String nick) {
        if (isHost) {
            Thread serverThread = new Thread(() -> {
                try {
                    new PokerServer().start();
                } catch (IOException e) { e.printStackTrace(); }
            }, "PokerServer");
            serverThread.setDaemon(true);
            serverThread.start();
        }

        PokerClient client = new PokerClient(app);
        GameScreen gameScreen = new GameScreen(app, client, isHost);
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            app.getAndroidBridge().hideKeyboard();
        }
        app.setScreen(gameScreen);

        Thread clientThread = new Thread(() -> {
            try {
                client.start(ip, nick);
            } catch (IOException e) {
                Gdx.app.postRunnable(() ->
                    app.setScreen(new LobbyScreen(app, "Failed to connect…"))
                );
            }
        }, "PokerClient");
        clientThread.setDaemon(true);
        clientThread.start();
    }


    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        if (PokerServer.getInstance() != null) {
            PokerServer.getInstance().shutdownServer();
        }
    }
}
