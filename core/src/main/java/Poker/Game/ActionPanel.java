package Poker.Game;

import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Actor;


public class ActionPanel extends Table {
    public interface ActionListener {
        void onFold();
        void onCall();
        void onCheck();
        void onRaise(int amount);
    }

    private final TextButton foldBtn, checkOrCallBtn, raiseBtn;
    private final Slider raiseSlider;
    private final Label raiseAmountLabel;
    private final ActionListener listener;

    public ActionPanel(Skin skin, ActionListener listener, float uiScale) {
        this.listener = listener;

        foldBtn = new TextButton("Fold", skin);
        checkOrCallBtn = new TextButton("Check", skin);
        raiseBtn = new TextButton("Raise", skin);
        raiseSlider = new Slider(20, 1000, 10, false, skin);
        raiseAmountLabel = new Label("20$", skin);

        foldBtn.setName("Fold");
        checkOrCallBtn.setName("CheckOrCall");
        raiseBtn.setName("Raise");
        foldBtn.setTouchable(Touchable.enabled);
        checkOrCallBtn.setTouchable(Touchable.enabled);
        raiseBtn.setTouchable(Touchable.enabled);
        // Обязательно!

        // фиксированные слушатели
        foldBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                listener.onFold();
                hide();
            }
        });
        raiseBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                listener.onRaise((int) raiseSlider.getValue());
                hide();
            }
        });
        raiseSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                raiseAmountLabel.setText((int) raiseSlider.getValue() + "$");
            }
        });
        checkOrCallBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                switch(event.toString()) {
                    case "Call":
                        listener.onCall();
                        break;
                    case "Check":
                        listener.onCheck();
                        break;
                    default:
                        // Ничего или ошибка
                }
                hide();
            }
        });

        // лэйаут
        pad(uiScale * 10);
        defaults().space(uiScale * 5);

        add(raiseSlider).colspan(2).width(200 * uiScale).height(40 * uiScale);
        add(raiseAmountLabel).height(40 * uiScale);
        row();
        add(foldBtn).width(110 * uiScale).height(60 * uiScale);
        add(checkOrCallBtn).width(110 * uiScale).height(60 * uiScale);
        add(raiseBtn).width(110 * uiScale).height(60 * uiScale);

        pack();
        setVisible(false);
    }

    public void updateButtons(boolean canFold, boolean canCheck, boolean canCall, boolean canRaise) {
        foldBtn.setVisible(canFold);
        foldBtn.setDisabled(!canFold);
        foldBtn.setTouchable(canFold ? Touchable.enabled : Touchable.disabled);

        raiseBtn.setVisible(canRaise);
        raiseBtn.setDisabled(!canRaise);
        raiseBtn.setTouchable(canRaise ? Touchable.enabled : Touchable.disabled);

        //checkOrCallBtn.clearListeners();

        if (canCall) {
            checkOrCallBtn.setVisible(true);
            checkOrCallBtn.setText("Call");
            checkOrCallBtn.setDisabled(false);
            checkOrCallBtn.setTouchable(Touchable.enabled);
            checkOrCallBtn.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    listener.onCall();
                    hide();
                }
            });
        } else if (canCheck) {
            checkOrCallBtn.setVisible(true);
            checkOrCallBtn.setText("Check");
            checkOrCallBtn.setDisabled(false);
            checkOrCallBtn.setTouchable(Touchable.enabled);
            checkOrCallBtn.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    listener.onCheck();
                    hide();
                }
            });
        } else {
            checkOrCallBtn.setVisible(false);
            checkOrCallBtn.setDisabled(true);
            checkOrCallBtn.setTouchable(Touchable.disabled);
        }

        show();
        checkOrCallBtn.toFront();  // на всякий случай — подтолкнуть кнопку наверх

        invalidateHierarchy();
        pack();
    }



    public void show() {
        setVisible(true);
    }

    public void hide() {
        setVisible(false);
    }
}
