package com.Calorizer.Bot.Model.Enum;

public enum MainGoal {
    WEIGHT_LOSS(0.8),
    Maintenance(1),
    WEIGHT_GAIN(1.1);

    private final double factor;

    MainGoal(double factor) {
        this.factor = factor;
    }

    public double getFactor() {
        return factor;
    }
}
