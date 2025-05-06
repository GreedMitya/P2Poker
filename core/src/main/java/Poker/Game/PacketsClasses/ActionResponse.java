package Poker.Game.PacketsClasses;


/**
 * Ответ от клиента с выбранным действием.
 */
public class ActionResponse {
    /** ID соединения/игрока (Connection.getID()) */
    public int playerId;
    /** Выбранное действие */
    public Action chosenAction;

    public double amount;

    public ActionResponse() {}
}
