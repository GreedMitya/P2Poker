package Poker.Game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import Poker.Game.Client.PokerClient;
import Poker.Game.PacketsClasses.ServerDiscoverer;
import Poker.Game.Server.PokerServer;
import com.esotericsoftware.kryonet.Client;
import com.badlogic.gdx.Game;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import static com.badlogic.gdx.Gdx.app;

public class PokerApp extends Game {
    @Override
    public void create() {
        setScreen(new LobbyScreen(this));
    }
}



