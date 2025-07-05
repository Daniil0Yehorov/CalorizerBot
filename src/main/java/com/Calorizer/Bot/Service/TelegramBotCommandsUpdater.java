package com.Calorizer.Bot.Service;

import com.Calorizer.Bot.Model.Enum.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeChat;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

/**
 * Service responsible for updating the list of Telegram bot commands
 * displayed to the user in their client application.
 * It uses {@link LocalizationService} to get language-specific commands.
 */
@Service
public class TelegramBotCommandsUpdater {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotCommandsUpdater.class);
    private final LocalizationService localizationService;

    /**
     * Constructor for dependency injection.
     *
     * @param localizationService Service to retrieve localized bot commands.
     */
    public TelegramBotCommandsUpdater(LocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    /**
     * Updates the custom commands menu for a specific chat/user in the Telegram client.
     * The commands are localized based on the provided language.
     *
     * @param absSender The {@link AbsSender} instance (the bot itself) used to execute API methods.
     * @param chatId The Telegram chat ID for which to update the commands.
     * @param language The {@link Language} to retrieve the localized commands for.
     */
    public void updateCommands(AbsSender absSender, long chatId, Language language) {
        List<BotCommand> commands = localizationService.getLocalizedCommands(language);
        try {
            absSender.execute(new SetMyCommands(commands, new BotCommandScopeChat(String.valueOf(chatId)), null));
        } catch (TelegramApiException e) {
            logger.error("Failed to update commands for chat {}: {}", chatId, e.getMessage());
        }
    }
}