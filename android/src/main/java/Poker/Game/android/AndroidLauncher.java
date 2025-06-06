package Poker.Game.android;

import Poker.Game.PokerApp;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.view.View;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;


/** Launches the Android application. */
public class AndroidLauncher extends AndroidApplication {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        configuration.useImmersiveMode = true; // Рекомендуется, но не обязательно.

        // Инициализация вашего приложения
        initialize(new PokerApp(), configuration);

        // Скрытие клавиатуры, если она открыта при старте
        hideKeyboard();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }
    

    // Также можно скрыть клавиатуру при потере фокуса
    @Override
    public void onPause() {
        super.onPause();
        hideKeyboard();
    }
}
