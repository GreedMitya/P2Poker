package Poker.Game.Client;

import Poker.Game.PacketsClasses.Logger;
import com.esotericsoftware.kryonet.Server;

import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ServerDiscoverer {
    public ServerDiscoverer(){}

    private static final int DISCOVERY_PORT = 8888;
    private static final String DISCOVERY_REQUEST = "POKER_DISCOVER";
    private static final String DISCOVERY_RESPONSE_PREFIX = "POKER_SERVER:";

    public static List<String> discoverServers(int timeoutMillis) {
        List<String> foundServers = new ArrayList<>();

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);

            byte[] requestData = DISCOVERY_REQUEST.getBytes();
            DatagramPacket requestPacket = new DatagramPacket(
                requestData,
                requestData.length,
                InetAddress.getByName("255.255.255.255"),
                DISCOVERY_PORT
            );

            socket.send(requestPacket);
            Logger.client("📡 Broadcast отправлен");

            long startTime = System.currentTimeMillis();
            byte[] buffer = new byte[1500];

            while (System.currentTimeMillis() - startTime < timeoutMillis) {
                DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                socket.setSoTimeout(timeoutMillis);
                try {
                    socket.receive(responsePacket);
                    String message = new String(responsePacket.getData(), 0, responsePacket.getLength());
                    if (message.startsWith(DISCOVERY_RESPONSE_PREFIX)) {
                        String serverIP = message.substring(DISCOVERY_RESPONSE_PREFIX.length());
                        if (!foundServers.contains(serverIP)) {
                            foundServers.add(serverIP);
                            Logger.client("🛰️ Найден сервер: " + serverIP);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    // Таймаут — просто выходим
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("💥 Ошибка поиска сервера: " + e.getMessage());
        }

        return foundServers;
    }
}
