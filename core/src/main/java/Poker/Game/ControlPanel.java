package Poker.Game;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class ControlPanel extends Table {
    public final TextButton startBtn;
    public final TextButton restartBtn;
    public final TextButton disconnectBtn;

    public ControlPanel(Skin skin, float uiScale, ControlListener listener) {
        startBtn = new TextButton("Start", skin);
        restartBtn = new TextButton("Restart", skin);
        disconnectBtn = new TextButton("Disconnect", skin);
        restartBtn.setVisible(false);
        startBtn.setVisible(false);
        disconnectBtn.setVisible(true);

        pad(uiScale);
        defaults().space(uiScale);

        add(restartBtn).width(120 * uiScale).height(50 * uiScale);
        add(startBtn).width(120 * uiScale).height(50 * uiScale);
        row();
        add(disconnectBtn).width(200 * uiScale).height(50 * uiScale);
        pack();
        startBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                listener.onStart();
            }
        });
        disconnectBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                listener.onDisconnect();
            }
        });
        restartBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                listener.onRestart();
            }
        });
    }
    public interface ControlListener {
        void onStart();
        void onRestart();
        void onDisconnect();
    }
}

