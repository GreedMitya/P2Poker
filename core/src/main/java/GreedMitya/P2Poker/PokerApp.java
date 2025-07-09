package GreedMitya.P2Poker;

import GreedMitya.P2Poker.Server.PokerServer;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;

public class PokerApp extends Game {
    public SoundManager sounds;
    private AndroidBridge androidBridge;
    public PokerApp(AndroidBridge bridge) {
        this.androidBridge = bridge;
    }
    public PokerApp() {
    }
    public AndroidBridge getAndroidBridge() {
        return androidBridge;
    }
    @Override
    public void create() {
        //sounds = new SoundManager();
        setScreen(new LobbyScreen(this));
        SoundManager.getInstance();
    }
    @Override
    public void dispose() {
        super.dispose();
        if (sounds != null) sounds.dispose();
        System.out.println("[APP] Disposing...");
        // Останавливаем сервер, если он запущен
        if (PokerServer.getInstance() != null) {
            PokerServer.getInstance().shutdownServer();
        }
        // Если это Android-версия и мост установлен — закрываем через него
        if (androidBridge != null && Gdx.app.getType() == Application.ApplicationType.Android) {
            androidBridge.exitApp();
        }
        // На десктопе просто выходим из VM
        if (Gdx.app.getType() != Application.ApplicationType.Android) {
            Gdx.app.exit();   // гарантированно вызовет завершение Lwjgl3Application
        }
    }
}



