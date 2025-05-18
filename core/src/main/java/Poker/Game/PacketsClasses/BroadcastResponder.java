package Poker.Game.PacketsClasses;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class BroadcastResponder {
    private static Thread listenerThread;
    private static DatagramSocket socket;

    public static void startListening() {
        if (listenerThread != null && listenerThread.isAlive()) return;

        listenerThread = new Thread(() -> {
            try {
                socket = new DatagramSocket(8888);
                byte[] buffer = new byte[1500];

                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);  // тут бросит SocketException, когда закроем сокет

                    String message = new String(packet.getData(), 0, packet.getLength());
                    if ("POKER_DISCOVER".equals(message)) {
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
                if (!(e instanceof java.net.SocketException && "socket closed".equals(e.getMessage()))) {
                    System.err.println("💥 Ошибка broadcast-сервера: " + e.getMessage());
                }
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }
        }, "BroadcastResponder");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public static void stopListening() {
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
