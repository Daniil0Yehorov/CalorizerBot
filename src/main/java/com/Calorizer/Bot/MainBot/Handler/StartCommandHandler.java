package com.Calorizer.Bot.MainBot.Handler;
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

import java.text.MessageFormat;

/**
 * Handles the '/start' command.
 * This handler greets the user, ensures their profile exists (or creates a new one),
 * and updates the bot's command list in the Telegram client to match the user's language.
 */
@Component
public class StartCommandHandler implements CommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(StartCommandHandler.class);

    private final UserServiceInt userServiceInt;
    private final LocalizationService localizationService;
    private final MessageSender messageSender;
    private final TelegramBotCommandsUpdater commandsUpdater;

    /**
     * Constructor for dependency injection.
     *
     * @param userServiceInt Service for user-related operations (e.g., getting or creating user).
     * @param localizationService Service for retrieving localized messages.
     * @param messageSender Service for sending messages to Telegram.
     * @param commandsUpdater Service responsible for updating bot commands in Telegram UI.
     */
    public StartCommandHandler(UserServiceInt userServiceInt,
                               LocalizationService localizationService,
                               MessageSender messageSender,
                               TelegramBotCommandsUpdater commandsUpdater) {
        this.userServiceInt = userServiceInt;
        this.localizationService = localizationService;
        this.messageSender = messageSender;
        this.commandsUpdater = commandsUpdater;
    }

    /**
     * Checks if this handler supports the given command text.
     * It specifically handles the "/start" command.
     *
     * @param commandText The text of the command received from the user.
     * @return true if the command text is "/start", false otherwise.
     */
    @Override
    public boolean supports(String commandText) {
        return "/start".equals(commandText);
    }

    /**
     * Processes the '/start' command.
     * It retrieves or creates the user, updates bot commands, and sends a personalized greeting.
     *
     * @param absSender The {@link AbsSender} instance for sending Telegram responses.
     * @param update The {@link Update} object containing the command message.
     */
    @Override
    public void handle(AbsSender absSender, Update update) {
        long chatId = update.getMessage().getChatId();
        String username = update.getMessage().getChat().getFirstName();

        User user = userServiceInt.getOrCreateUser(chatId);

        commandsUpdater.updateCommands(absSender, chatId, user.getLanguage());

        String greetingTemplate = localizationService.getTranslation(user.getLanguage(), "greeting");
        String text = MessageFormat.format(greetingTemplate, username);

        messageSender.sendMessage(absSender, chatId, text);
        logger.info("Replied to user {} with /start command.", username);
    }
}