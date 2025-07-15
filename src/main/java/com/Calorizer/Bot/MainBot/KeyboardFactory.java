package com.Calorizer.Bot.MainBot;

import com.Calorizer.Bot.Model.Enum.Language;
import com.Calorizer.Bot.Service.LocalizationService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import java.util.ArrayList;
import java.util.List;

/**
 * A factory class responsible for creating and configuring Telegram {@link ReplyKeyboardMarkup}s.
 * This class uses {@link LocalizationService} to provide localized button texts,
 * ensuring the keyboard adapts to the user's selected language.
 */
@Component
public class KeyboardFactory {

    private final LocalizationService localizationService;

    /**
     * Constructs a new KeyboardFactory instance.
     *
     * @param localizationService Service for retrieving localized strings, used to set button texts.
     */
    public KeyboardFactory(LocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    /**
     * Creates and returns a {@link ReplyKeyboardMarkup} representing the main menu of the bot.
     * The buttons on this keyboard are localized according to the provided {@link Language}.
     *
     * The keyboard is configured to:
     * - {@code setResizeKeyboard(true)}: Adjust its size to fit the screen.
     * - {@code setOneTimeKeyboard(false)}: Remain visible after a button is pressed (not disappear).
     *
     * @param lang The {@link Language} for which the keyboard button texts should be localized.
     * @return A {@link ReplyKeyboardMarkup} configured with main menu buttons.
     */
    public ReplyKeyboardMarkup createMainMenuKeyboard(Language lang) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();

        row1.add(new KeyboardButton(localizationService.getTranslation(lang, "button.command.profile")));
        row1.add(new KeyboardButton(localizationService.getTranslation(lang, "button.command.calculatecalorieforday")));
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton(localizationService.getTranslation(lang, "button.command.recommendation_static")));
        row2.add(new KeyboardButton(localizationService.getTranslation(lang, "button.command.recommendation_ai")));
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton(localizationService.getTranslation(lang, "button.command.changelanguage")));
        keyboard.add(row3);

        replyKeyboardMarkup.setKeyboard(keyboard);
        return replyKeyboardMarkup;
    }
}