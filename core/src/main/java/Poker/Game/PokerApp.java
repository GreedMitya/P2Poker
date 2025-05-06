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

public class PokerApp extends Game {
    @Override
    public void create() {
        setScreen(new LobbyScreen(this));
    }

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Выберите режим: (1) Хост, (2) Клиент");
        int choice = scanner.nextInt();
        scanner.nextLine(); // съедаем \n

        System.out.print("Введите ваш никнейм: ");
        String nickname = scanner.nextLine();

        if (choice == 1) {
            PokerServer server = new PokerServer();
            new Thread(() -> {
                try {
                    server.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            PokerClient client = new PokerClient();
            client.start("localhost", nickname);


        } else {
            try {
                List<String> servers = ServerDiscoverer.discoverServers(3000); // 3 секунды
                if (!servers.isEmpty()) {
                    System.out.println("LAN servers: " + servers);
                    String ip = servers.get(0);// Можно показать пользователю список
                    PokerClient client = new PokerClient();
                    client.setHost(true);
                    client.start(ip, nickname);
                } else {
                    System.out.println("❌ Серверы не найдены");
                    System.out.print("Введите IP хоста: ");
                    String ip = scanner.nextLine();
                    PokerClient client = new PokerClient();
                    client.setHost(false);
                    client.start(ip, nickname);

                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}



