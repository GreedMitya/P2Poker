package GreedMitya.P2Poker.PacketsClasses;

public class Logger {
    public Logger(){
    }
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String BLUE = "\u001B[34m";
    private static final String YELLOW = "\u001B[33m";

    public static void server(String msg) {
        System.out.println(GREEN + "[SERVER] " + msg + RESET);
    }

    public static void client(String msg) {
        System.out.println(BLUE + "[CLIENT] " + msg + RESET);
    }

    public static void Game(String msg) {
        System.out.println(YELLOW + "[GAME] " + msg + RESET);
    }
}
