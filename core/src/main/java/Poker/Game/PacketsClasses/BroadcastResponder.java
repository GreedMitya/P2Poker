package Poker.Game.PacketsClasses;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class BroadcastResponder {

    public static void startListening() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(8888)) {
                byte[] buffer = new byte[1500];

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength());
                    if (message.equals("POKER_DISCOVER")) {
                        String response = "POKER_SERVER:" + InetAddress.getLocalHost().getHostAddress();
                        byte[] responseData = response.getBytes();
                        DatagramPacket responsePacket = new DatagramPacket(
                            responseData, responseData.length,
                            packet.getAddress(), packet.getPort()
                        );
                        socket.send(responsePacket);
                        Logger.server("📨 Ответил на broadcast запрос от: " + packet.getAddress());
                    }
                }

            } catch (Exception e) {
                System.err.println("💥 Ошибка broadcast-сервера: " + e.getMessage());
            }
        }).start();
    }
}

