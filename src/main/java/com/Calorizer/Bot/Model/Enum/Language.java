package com.Calorizer.Bot.Model.Enum;

import java.util.Locale;

public enum Language {
    English(Locale.ENGLISH),
    Ukrainian(new Locale("uk", "UA")),
    Russian(new Locale("ru", "RU")),
    German(Locale.GERMAN);

    private final Locale locale;

    Language(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }
}
