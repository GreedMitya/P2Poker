package Poker.Game.PacketsClasses;

public class Action {
    public String name;
    public double amount;
    public double min;
    public double max;

    // 1) Пустой конструктор — нужен KryoNet
    public Action() {}

    // 2) Конструктор для «простейших» ходов (fold, check и т.п.)
    public Action(String name) {
        this.name = name;
        this.amount = 0;
        this.min = 0;
        this.max = 0;
    }

    // 3) Конструктор, когда нужны все параметры
    public Action(String name, double amount, double min, double max) {
        this.name   = name;
        this.amount = amount;  // конкретная ставка (для call/all-in)
        this.min    = min;     // минимальный raise
        this.max    = max;     // максимальный raise (обычно — баланс игрока)
    }
}
