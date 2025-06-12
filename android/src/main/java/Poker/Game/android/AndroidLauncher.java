package Poker.Game.android;

import Poker.Game.AndroidBridge;
import Poker.Game.PokerApp;
import android.os.Build;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class AndroidLauncher extends AndroidApplication {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        configuration.useImmersiveMode = true;
        // Создаём bridge и передаём в PokerApp
        AndroidBridge bridge = new AndroidBridge() {
            @Override
            public void exitApp() {
                runOnUiThread(() -> {
                    Gdx.app.exit();  // вызовет dispose() в PokerApp

                    // закрыть таск и убрать из «Недавних»
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        finishAndRemoveTask();
                    } else {
                        finish();
                    }

                    // окончательно убить процесс
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                });
            }

        };
        initialize(new PokerApp(bridge), configuration);
        hideKeyboard();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        hideKeyboard();
    }
}
