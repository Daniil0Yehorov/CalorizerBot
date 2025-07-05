package com.Calorizer.Bot.MainBot.Callback;

import com.Calorizer.Bot.Model.Enum.Language;
import com.Calorizer.Bot.Model.User;
import com.Calorizer.Bot.Service.Interface.UserServiceInt;
import com.Calorizer.Bot.Service.LocalizationService;
import com.Calorizer.Bot.Service.MessageSender;
import com.Calorizer.Bot.Service.TelegramBotCommandsUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Handles callback queries for language selection (e.g., "SET_LANGUAGE_English").
 * It updates the user's language preference and refreshes bot commands accordingly.
 */
@Component
public class LanguageCallback implements CallbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(LanguageCallback.class);

    private final UserServiceInt userServiceInt;
    private final LocalizationService localizationService;
    private final MessageSender messageSender;
    private final TelegramBotCommandsUpdater commandsUpdater;

    /**
     * Constructor for dependency injection.
     *
     * @param userServiceInt Service for user-related database operations.
     * @param localizationService Service for obtaining localized strings.
     * @param messageSender Service for sending messages to the user.
     * @param commandsUpdater Service for updating bot commands in Telegram UI.
     */
    public LanguageCallback(UserServiceInt userServiceInt,
                            LocalizationService localizationService,
                            MessageSender messageSender,
                            TelegramBotCommandsUpdater commandsUpdater) {
        this.userServiceInt = userServiceInt;
        this.localizationService = localizationService;
        this.messageSender = messageSender;
        this.commandsUpdater = commandsUpdater;
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
     * refreshes bot commands, and sends a confirmation message.
     * Handles cases where an unsupported language code is received.
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
     * Updates the user's language preference in the database and refreshes bot commands.
     *
     * @param absSender The {@link AbsSender} instance.
     * @param chatId The chat ID of the user.
     * @param newLanguage The newly selected {@link Language}.
     * @param oldLanguage The user's previous {@link Language} (used for the confirmation message).
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

        messageSender.sendMessage(absSender, chatId, confirmation);
    }
}