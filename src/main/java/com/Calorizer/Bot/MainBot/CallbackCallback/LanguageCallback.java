package com.Calorizer.Bot.MainBot.CallbackCallback;

import com.Calorizer.Bot.MainBot.KeyboardFactory;
import com.Calorizer.Bot.Model.Enum.Language;
import com.Calorizer.Bot.Model.User;
import com.Calorizer.Bot.Service.Interface.UserServiceInt;
import com.Calorizer.Bot.Service.LocalizationService;
import com.Calorizer.Bot.Service.MessageSender;
import com.Calorizer.Bot.Service.TelegramBotCommandsUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Handles callback queries for language selection (e.g., "SET_LANGUAGE_English").
 * It updates the user's language preference, refreshes bot commands,
 * and updates the main menu keyboard to reflect the new language.
 */
@Component
public class LanguageCallback implements CallbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(LanguageCallback.class);

    private final UserServiceInt userServiceInt;
    private final LocalizationService localizationService;
    private final MessageSender messageSender;
    private final TelegramBotCommandsUpdater commandsUpdater;
    private  final KeyboardFactory keyboardFactory;

    /**
     * Constructor for dependency injection.
     *
     * @param userServiceInt Service for user-related database operations.
     * @param localizationService Service for obtaining localized strings.
     * @param messageSender Service for sending messages to the user.
     * @param commandsUpdater Service for updating bot commands in Telegram UI.
     * @param keyboardFactory Factory for creating custom Telegram keyboards, specifically the main menu.
     */
    public LanguageCallback(UserServiceInt userServiceInt,
                            LocalizationService localizationService,
                            MessageSender messageSender,
                            TelegramBotCommandsUpdater commandsUpdater,KeyboardFactory keyboardFactory) {
        this.userServiceInt = userServiceInt;
        this.localizationService = localizationService;
        this.messageSender = messageSender;
        this.commandsUpdater = commandsUpdater;
        this.keyboardFactory=keyboardFactory;
    }

    /**
     * Determines if this handler supports the given callback data.
     * It supports any callback data starting with "SET_LANGUAGE_".
     *
     * @param callbackData The data string from the callback query.
     * @return true if the data indicates a language selection, false otherwise.
     */
    @Override
    public boolean supports(String callbackData) {
        return callbackData.startsWith("SET_LANGUAGE_");
    }

    /**
     * Processes the language selection callback.
     * Extracts the new language from callback data, updates user's language,
     * refreshes bot commands, and sends a confirmation message along with the updated main keyboard.
     * Handles cases where an unsupported language code is received, sending an appropriate error message.
     *
     * @param absSender The {@link AbsSender} instance for sending Telegram responses.
     * @param update The {@link Update} object containing the callback query.
     */
    @Override
    public void handle(AbsSender absSender, Update update) {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String data = update.getCallbackQuery().getData();
        User user = userServiceInt.getOrCreateUser(chatId);
        Language currentUserLanguage = user.getLanguage();

        String langCode = data.replace("SET_LANGUAGE_", "");
        try {
            Language selectedLang = Language.valueOf(langCode);
            updateUserLanguage(absSender, chatId, selectedLang, currentUserLanguage);
            logger.info("User {} changed language to {} via callback.", chatId, selectedLang.name());
        } catch (IllegalArgumentException e) {
            messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(currentUserLanguage, "language.unsupported"));
            logger.warn("User {} tried to set unsupported language via callback: {}", chatId, langCode);
        }
    }

    /**
     * Updates the user's language preference in the database, refreshes bot commands
     * in the Telegram UI, and sends a confirmation message with the main menu keyboard
     * updated to the new language.
     *
     * @param absSender The {@link AbsSender} instance for sending Telegram responses.
     * @param chatId The chat ID of the user whose language is being updated.
     * @param newLanguage The newly selected {@link Language} to set for the user.
     * @param oldLanguage The user's previous {@link Language} (primarily for logging or if a specific "reverted" message is needed,
     * but the confirmation will be in the new language).
     */
    private void updateUserLanguage(AbsSender absSender, Long chatId, Language newLanguage, Language oldLanguage) {
        User user = userServiceInt.getOrCreateUser(chatId);
        user.setLanguage(newLanguage);
        userServiceInt.save(user);

        commandsUpdater.updateCommands(absSender, chatId, newLanguage);

        String confirmation;
        switch (newLanguage) {
            case Ukrainian:
                confirmation = localizationService.getTranslation(oldLanguage, "language.set.ukrainian");
                break;
            case Russian:
                confirmation = localizationService.getTranslation(oldLanguage, "language.set.russian");
                break;
            case German:
                confirmation = localizationService.getTranslation(oldLanguage, "language.set.german");
                break;
            default:
                confirmation = localizationService.getTranslation(oldLanguage, "language.set.english");
                break;
        }
        SendMessage replyMessage = new SendMessage();
        replyMessage.setChatId(String.valueOf(chatId));
        replyMessage.setText(confirmation);
        replyMessage.setReplyMarkup(keyboardFactory.createMainMenuKeyboard(newLanguage));
        messageSender.sendMessage(absSender,replyMessage);
    }
}