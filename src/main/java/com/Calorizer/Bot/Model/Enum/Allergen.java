package com.Calorizer.Bot.Model.Enum;

/**
 * Defines the set of common food allergens.
 * Each constant in this enum represents a specific allergen that users might select
 * when specifying their dietary restrictions or sensitivities.
 *
 * <p>This enum is designed to be straightforward and easily extendable by simply
 * adding new allergen constants as needed, without requiring changes to existing logic
 * that processes these allergens (e.g., in {@link com.Calorizer.Bot.Service.AllergenListConverter}).</p>
 */
public enum Allergen {
    MILK,
    EGGS,
    PEANUTS,
    TREE_NUTS,
    WHEAT,
    SOY,
    FISH,
    SHELLFISH,
    SESAME
}