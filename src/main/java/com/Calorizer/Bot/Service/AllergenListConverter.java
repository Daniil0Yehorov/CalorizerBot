package com.Calorizer.Bot.Service;

import com.Calorizer.Bot.Model.Enum.Allergen;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * JPA Converter for converting a List of Allergen enum values to a single String for database storage
 * and vice versa. This allows storing multiple enum values in a single database column
 * (e.g., as a comma-separated string) without requiring a separate join table.
 *
 * Implements the Single Responsibility Principle (SRP) by focusing solely on this conversion logic.
 */
@Converter
public class AllergenListConverter implements AttributeConverter<List<Allergen>, String> {

    /**
     * Converts a List of Allergen enums to a String representation for database storage.
     * The allergens are joined by a comma.
     *
     * @param attribute The List of Allergen enums to convert. Can be null or empty.
     * @return A comma-separated String of allergen names, or an empty string if the input list is null or empty.
     */
    @Override
    public String convertToDatabaseColumn(List<Allergen> attribute) {

        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        return attribute.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    /**
     * Converts a String representation from the database back into a List of Allergen enums.
     * The string is expected to be a comma-separated list of Allergen enum names.
     *
     * @param dbData The String from the database column. Can be null or an empty string.
     * @return A List of Allergen enums. Returns an empty, unmodifiable list if the input string is null, empty,
     * or contains no valid allergen names.
     * Note: Uses Collectors.toUnmodifiableList() for immutability, promoting safer data handling.
     */
    @Override
    public List<Allergen> convertToEntityAttribute(String dbData) {

        if (dbData == null || dbData.trim().isEmpty()) {
            return List.of();
        }

        return Arrays.stream(dbData.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::safeValueOfAllergen)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Helper method to safely convert a string to an Allergen enum,
     * returning null if the string does not match any enum constant.
     * This prevents IllegalArgumentException during parsing.
     *
     * @param name The string name of the allergen.
     * @return The corresponding Allergen enum, or null if no match is found.
     */
    private Allergen safeValueOfAllergen(String name) {
        try {
            return Allergen.valueOf(name);
        } catch (IllegalArgumentException e) {
             System.err.println("Warning: Invalid allergen name found in database: " + name);
            return null;
        }
    }
}