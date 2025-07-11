package GreedMitya.P2Poker;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ExtendViewport;


public class WinnerScreen implements Screen {
    private final PokerApp app;
    private Stage stage;
    private ShapeRenderer shapeRenderer;
    private float timer = 5f;

    private static class ConfettiParticle {
        float x, y, speedY;
        Color color;
        ConfettiParticle(float x, float y, float speedY, Color color) {
            this.x = x; this.y = y;
            this.speedY = speedY; this.color = color;
        }
        void update(float delta, float worldHeight) {
            y -= speedY * delta;
            if (y < 0) y = worldHeight + (float)(Math.random() * 50);
        }
    }

    private final ConfettiParticle[] confetti = new ConfettiParticle[100];

    public WinnerScreen(PokerApp app, String winnerName) {
        this.app = app;
        stage = new Stage(new ExtendViewport(480, 800));
        Gdx.input.setInputProcessor(stage);


        Skin skin = new Skin(Gdx.files.internal("sgx/skin/sgx-ui.json"),
            new TextureAtlas(Gdx.files.internal("sgx/skin/sgx-ui.atlas")));
        Label.LabelStyle style = new Label.LabelStyle(skin.getFont("title"), Color.GOLD);
        Label label = new Label("🎉 " + winnerName + " wins! 🎉", style);
        label.setFontScale(1.5f);

        Table table = new Table();
        table.setFillParent(true);
        table.center();
        table.add(label);
        stage.addActor(table);

        float worldW = stage.getViewport().getWorldWidth();
        float worldH = stage.getViewport().getWorldHeight();
        for (int i = 0; i < confetti.length; i++) {
            confetti[i] = new ConfettiParticle(
                (float)(Math.random() * worldW),
                (float)(Math.random() * worldH),
                30 + (float)(Math.random() * 70),
                new Color((float)Math.random(), (float)Math.random(), (float)Math.random(), 1f)
            );
        }

        shapeRenderer = new ShapeRenderer();
        SoundManager.getInstance().play("winner", 0.8f);

    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        timer -= delta;
        if (timer <= 0) {
            app.setScreen(new LobbyScreen(app));
            dispose();
            return;
        }

        stage.getViewport().apply();
        Gdx.gl.glClearColor(0, 0, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        float worldW = stage.getViewport().getWorldWidth();
        float worldH = stage.getViewport().getWorldHeight();
        for (ConfettiParticle p : confetti) p.update(delta, worldH);

        shapeRenderer.setProjectionMatrix(stage.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (ConfettiParticle p : confetti) {
            shapeRenderer.setColor(p.color);
            shapeRenderer.circle(p.x, p.y, 4f);
        }
        shapeRenderer.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        shapeRenderer.dispose();
    }
}
