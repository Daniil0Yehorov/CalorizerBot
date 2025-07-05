package com.Calorizer.Bot.Model.Enum;

/**
 * Enumerates the main fitness goals a user might have.
 * Each goal is associated with a numerical factor used in calorie calculations.
 */
public enum MainGoal {
    WEIGHT_LOSS(0.8),
    Maintenance(1),
    WEIGHT_GAIN(1.1);

    private final double factor;

    /**
     * Constructor for the MainGoal enum.
     *
     * @param factor The numerical factor used in calorie calculations for this goal.
     */
    MainGoal(double factor) {
        this.factor = factor;
    }

    /**
     * Returns the calorie adjustment factor for this main goal.
     *
     * @return The factor value.
     */
    public double getFactor() {
        return factor;
    }
}
