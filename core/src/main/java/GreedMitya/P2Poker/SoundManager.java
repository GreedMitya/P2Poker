package GreedMitya.P2Poker;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.SoundLoader;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Sound;

import java.util.HashMap;
import java.util.Map;

public class SoundManager {
    private static SoundManager instance;
    private static final String[] NAMES = {
        "flipcard",
        "bet",
        "fold",
        "check",
        "winner",
        "totalpot1",
        "flipontable"
        , "enterance"
    };
    private final AssetManager assets;
    private final Map<String, Sound> sounds = new HashMap<>();

    private SoundManager() {
        assets = new AssetManager();
        assets.setLoader(Sound.class, new SoundLoader(new InternalFileHandleResolver()));
        for (String name : NAMES) {
            assets.load("sounds/" + name + ".wav", Sound.class);
        }
        assets.finishLoading();
        for (String name : NAMES) {
            sounds.put(name, assets.get("sounds/" + name + ".wav", Sound.class));
        }
    }

    /** Воспроизвести звук по имени, громкость 0.0–1.0 */
    public void play(String name, float volume) {
        Sound s = sounds.get(name);
        if (s != null) s.play(volume);
    }
    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    public void dispose() {
        assets.dispose();
    }
}
