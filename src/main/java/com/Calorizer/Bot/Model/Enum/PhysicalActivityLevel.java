package com.Calorizer.Bot.Model.Enum;

/**
 * Enumerates different levels of physical activity.
 * Each level is associated with a numerical factor used in basal metabolic rate (BMR)
 * and total daily energy expenditure (TDEE) calculations.
 */
public enum PhysicalActivityLevel {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    ACTIVE(1.725),
    VERY_ACTIVE(1.9);

    private final double factor;

    /**
     * Constructor for the PhysicalActivityLevel enum.
     *
     * @param factor The numerical factor representing the activity level for calorie calculations.
     */
    PhysicalActivityLevel(double factor) {
        this.factor = factor;
    }

    /**
     * Returns the activity factor for this physical activity level.
     *
     * @return The factor value.
     */
    public double getFactor() {
        return factor;
    }
}
