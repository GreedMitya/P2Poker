package GreedMitya.P2Poker.PacketsClasses;

public class Action {
    public String name;
    public double amount;
    public double min;
    public double max;
    public boolean allIn;


    public Action() {}


    public Action(String name) {
        this.name = name;
        this.amount = 0;
        this.min = 0;
        this.max = 0;
        this.allIn = false;
    }


    public Action(String name, double amount, double min, double max) {
        this.name   = name;
        this.amount = amount;
        this.min    = min;
        this.max    = max;
        this.allIn = false;
    }
    public Action(String name, double amount, double min, double max, boolean allIn) {
        this.name   = name;
        this.amount = amount;
        this.min    = min;
        this.max    = max;
        this.allIn  = allIn;
    }
}
