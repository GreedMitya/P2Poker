package Poker.Game.PacketsClasses;


/**
 * Запрос от сервера к клиенту дать ход.
 */
public class ActionRequest {
    /** ID соединения/игрока (Connection.getID()) */
    public int playerId;
    /** Список доступных действий */
    public Action[] availableActions;
    /** Таймаут в секундах */
    public int timeoutSec;

    // Нужен публичный конструктор без аргументов для Kryo
    public ActionRequest() {}
}
