package com.Calorizer.Bot.MainBot.Handler;

import com.Calorizer.Bot.Model.Enum.Language;
import com.Calorizer.Bot.Service.Interface.UserServiceInt;
import com.Calorizer.Bot.Service.LocalizationService;
import com.Calorizer.Bot.Service.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the '/changelanguage' command.
 * This handler sends a message to the user with an inline keyboard for language selection.
 * The actual language update logic is handled by {@link com.Calorizer.Bot.MainBot.Callback.LanguageCallback}.
 */
@Component
public class LanguageHandler implements CommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(LanguageHandler.class);

    private final LocalizationService localizationService;
    private final MessageSender messageSender;

    /**
     * Constructor for dependency injection.
     *
     * @param localizationService Service for obtaining localized strings.
     * @param messageSender Service for sending messages to the user.
     */
    public LanguageHandler(LocalizationService localizationService,
                           MessageSender messageSender) {
        this.localizationService = localizationService;
        this.messageSender = messageSender;
    }

    /**
     * Checks if this handler supports the given command text.
     * It specifically handles the "/changelanguage" command.
     *
     * @param commandText The text of the command received from the user.
     * @return true if the command text is "/changelanguage", false otherwise.
     */
    @Override
    public boolean supports(String commandText) {
        return "/changelanguage".equals(commandText);
    }

    /**
     * Handles the '/changelanguage' command.
     * It sends the language selection keyboard to the user.
     *
     * @param absSender The {@link AbsSender} instance for sending Telegram responses.
     * @param update The {@link Update} object containing the command message.
     */
    @Override
    public void handle(AbsSender absSender, Update update) {
        long chatId = update.getMessage().getChatId();
        sendLanguageSelectionKeyboard(absSender, chatId);
        logger.info("Sent language selection keyboard to user {} via /changelanguage command.", chatId);
    }

    /**
     * Constructs and sends the inline keyboard for language selection.
     * The prompt message is sent in English as a neutral default.
     *
     * @param absSender The {@link AbsSender} instance.
     * @param chatId The chat ID of the user to send the keyboard to.
     */
    private void sendLanguageSelectionKeyboard(AbsSender absSender, Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(localizationService.getTranslation(Language.English, "language.selection.prompt"));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(createLanguageButton("🇬🇧 English", Language.English)));
        rows.add(List.of(createLanguageButton("🇺🇦 Українська", Language.Ukrainian)));
        rows.add(List.of(createLanguageButton("🇷🇺 Русский", Language.Russian)));
        rows.add(List.of(createLanguageButton("🇩🇪 Deutsch", Language.German)));

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        messageSender.sendMessage(absSender, message);
    }

    /**
     * Helper method to create an inline keyboard button for language selection.
     *
     * @param text The display text on the button (e.g., "🇬🇧 English").
     * @param language The {@link Language} enum value associated with this button.
     * @return An {@link InlineKeyboardButton} configured with the text and a callback data.
     */
    private InlineKeyboardButton createLanguageButton(String text, Language language) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData("SET_LANGUAGE_" + language.name());
        return button;
    }
}