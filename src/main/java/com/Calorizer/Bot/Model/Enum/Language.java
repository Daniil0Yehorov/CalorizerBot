package com.Calorizer.Bot.Model.Enum;

import java.util.Locale;

/**
 * Enumerates the supported languages in the bot.
 * Each language is associated with a specific {@link Locale} object
 * for proper internationalization and localization.
 */
public enum Language {
    English(Locale.ENGLISH),
    Ukrainian(new Locale("uk", "UA")),
    Russian(new Locale("ru", "RU")),
    German(Locale.GERMAN);

    private final Locale locale;

    /**
     * Constructor for the Language enum.
     *
     * @param locale The {@link Locale} object corresponding to the language.
     */
    Language(Locale locale) {
        this.locale = locale;
    }

    /**
     * Returns the {@link Locale} object associated with this language.
     * This is useful for fetching localized resources or formatting dates/numbers.
     *
     * @return The {@link Locale} object for the language.
     */
    public Locale getLocale() {
        return locale;
    }
}
