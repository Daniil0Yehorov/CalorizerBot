package com.Calorizer.Bot.MainBot.CommandHandler;

import com.Calorizer.Bot.Model.Enum.Language;
import com.Calorizer.Bot.Model.User;
import com.Calorizer.Bot.Service.Interface.UserServiceInt;
import com.Calorizer.Bot.Service.LocalizationService;
import com.Calorizer.Bot.Service.MessageSender; // Используем MessageSender из Service
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.List;

/**
 * Handles unknown or unrecognized commands.
 * This handler serves as a fallback, informing the user about available commands
 * when a received message does not match any specific command handler.
 */
@Component
public class UnknownCommandHandler implements CommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(UnknownCommandHandler.class);

    private final UserServiceInt userServiceInt;
    private final LocalizationService localizationService;
    private final MessageSender messageSender;

    /**
     * Constructor for dependency injection.
     *
     * @param userServiceInt Service for user-related data operations.
     * @param localizationService Service for retrieving localized messages and commands.
     * @param messageSender Service for sending messages to Telegram.
     */
    public UnknownCommandHandler(UserServiceInt userServiceInt, LocalizationService localizationService, MessageSender messageSender) {
        this.userServiceInt = userServiceInt;
        this.localizationService = localizationService;
        this.messageSender = messageSender;
    }

    /**
     * This handler supports any command text, acting as a fallback for unhandled commands.
     * It should typically be the last handler in the chain to be checked.
     *
     * @param commandText The text of the command received from the user.
     * @return Always true, as it's designed to handle anything not caught by other handlers.
     */
    @Override
    public boolean supports(String commandText) {
        return true;
    }

    /**
     * Processes an unknown command.
     * Retrieves the user's language, fetches localized bot commands,
     * formats them into a message, and sends it to the user.
     *
     * @param absSender The {@link AbsSender} instance for sending Telegram responses.
     * @param update The {@link Update} object containing the unknown command message.
     */
    @Override
    public void handle(AbsSender absSender, Update update) {
        long chatId = update.getMessage().getChatId();
        User user = userServiceInt.getOrCreateUser(chatId);
        Language lang = user != null ? user.getLanguage() : Language.English;

        List<BotCommand> commands = localizationService.getLocalizedCommands(lang);
        StringBuilder builder = new StringBuilder();

        builder.append(localizationService.getTranslation(lang, "command.available_commands")).append("\n\n");

        for (BotCommand command : commands) {
            builder.append(command.getCommand())
                    .append(" - ")
                    .append(command.getDescription())
                    .append("\n");
        }

        messageSender.sendMessage(absSender, chatId, builder.toString());
        logger.warn("Received unknown command from user {}: {}", chatId, update.getMessage().getText());
    }
}