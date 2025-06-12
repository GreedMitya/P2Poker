package Poker.Game;

import com.badlogic.gdx.graphics.Color;
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

    private boolean isCallMode = false;

    private int minRaiseAmount = 20;
    private int maxRaiseAmount = 1000;


    private final TextButton foldBtn;
    private final TextButton checkOrCallBtn;
    private final TextButton raiseBtn;
    private final Slider raiseSlider;
    private final Label raiseAmountLabel;
    private final ActionListener listener;


    public void setMinRaise(int amount) {
        this.minRaiseAmount = amount;
        raiseSlider.setRange(minRaiseAmount, raiseSlider.getMaxValue());
        raiseSlider.setValue(minRaiseAmount);
        raiseAmountLabel.setText(minRaiseAmount + "$");
    }
    public void setRaiseRange(int min, int max) {
        this.minRaiseAmount = min;
        this.maxRaiseAmount = max;

        raiseSlider.setRange(minRaiseAmount, maxRaiseAmount);

        // ВСЕГДА сбрасываем значение на минимум
        raiseSlider.setValue(minRaiseAmount);
        raiseAmountLabel.setText(minRaiseAmount + "$");
    }


    public ActionPanel(Skin skin, ActionListener listener, float uiScale) {
        this.listener = listener;

        // Создаем кнопки и элементы управления
        foldBtn = new TextButton("Fold", skin);
        checkOrCallBtn = new TextButton("Check", skin);
        raiseBtn = new TextButton("Raise", skin);
        raiseSlider = new Slider(20, 1000, 10, false, skin);
        raiseAmountLabel = new Label("20$", skin);

        // Стилизация
        raiseAmountLabel.setFontScale(1.5f);
        raiseAmountLabel.setColor(Color.GREEN);

        // Названия для отладки
        foldBtn.setName("Fold");
        checkOrCallBtn.setName("CheckOrCall");
        raiseBtn.setName("Raise");

        // Поведение кнопок
        foldBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                listener.onFold();
                hide();
            }
        });

        // Универсальный слушатель для Check/Call
        checkOrCallBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (isCallMode) {
                    listener.onCall();
                } else {
                    listener.onCheck();
                }
                hide();
            }
        });


        raiseBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                listener.onRaise((int) raiseSlider.getValue());
                raiseSlider.setValue(minRaiseAmount); // Сброс на актуальный минимум
                raiseAmountLabel.setText(minRaiseAmount + "$");
                hide();
            }
        });



        raiseSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                raiseAmountLabel.setText((int) raiseSlider.getValue() + "$" );
            }
        });

        // Разметка
        padRight(25f * uiScale);
        add().width(0).height(0);
        add().width(0).height(0);
        add(raiseAmountLabel).height(20 * uiScale);
        row();
        add().width(0).height(0);
        add(raiseSlider).colspan(2).width(300 * uiScale).height(80 * uiScale);
        row();
        add(foldBtn).width(150 * uiScale).height(80 * uiScale);
        add(checkOrCallBtn).width(150 * uiScale).height(80 * uiScale);
        add(raiseBtn).width(150 * uiScale).height(80 * uiScale);

        pack();
        setVisible(false);
    }

    /**
     * Обновляет состояние кнопок в зависимости от возможностей игрока
     */
    public void updateButtons(boolean canFold, boolean canCheck, boolean canCall, boolean canRaise,
                              int minRaise, int maxRaise, int callAmount, boolean isAllIn) {
        foldBtn.setVisible(canFold);
        foldBtn.setDisabled(!canFold);
        foldBtn.setTouchable(canFold ? Touchable.enabled : Touchable.disabled);

        raiseBtn.setVisible(canRaise);
        raiseBtn.setDisabled(!canRaise);
        raiseBtn.setTouchable(canRaise ? Touchable.enabled : Touchable.disabled);

        if (canCall) {
            isCallMode = true;
            String callText = "Call " + callAmount + "$";
            if (isAllIn) {
                callText = "Call "+ "(All-in)";
                checkOrCallBtn.getLabel().setColor(Color.RED);
            } else {
                checkOrCallBtn.getLabel().setColor(Color.WHITE);
            }
            checkOrCallBtn.setText(callText);
            checkOrCallBtn.setVisible(true);
            checkOrCallBtn.setDisabled(false);
            checkOrCallBtn.setTouchable(Touchable.enabled);

        } else if (canCheck) {
            isCallMode = false;
            checkOrCallBtn.setText("Check");
            checkOrCallBtn.setVisible(true);
            checkOrCallBtn.setDisabled(false);
            checkOrCallBtn.setTouchable(Touchable.enabled);
        } else {
            checkOrCallBtn.setVisible(false);
            checkOrCallBtn.setDisabled(true);
            checkOrCallBtn.setTouchable(Touchable.disabled);
        }

        setRaiseRange(minRaise, maxRaise);
        setVisible(true);
        checkOrCallBtn.toFront();
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
