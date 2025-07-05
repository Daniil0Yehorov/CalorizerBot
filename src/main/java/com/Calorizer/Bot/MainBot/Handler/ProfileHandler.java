package com.Calorizer.Bot.MainBot.Handler;
import com.Calorizer.Bot.Service.Interface.UserServiceInt;
import com.Calorizer.Bot.Service.MessageSender; // Используем MessageSender из Service
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Handles the '/profile' command.
 * This handler retrieves and displays the user's profile information,
 * including access restrictions for paid accounts.
 */
@Component
public class ProfileHandler implements CommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProfileHandler.class);

    private final UserServiceInt userServiceInt;
    private final MessageSender messageSender;
    /**
     * Constructor for dependency injection.
     *
     * @param userServiceInt Service for user-related data operations, including profile retrieval.
     * @param messageSender Service for sending messages back to the user.
     */
    public ProfileHandler(UserServiceInt userServiceInt, MessageSender messageSender) {
        this.userServiceInt = userServiceInt;
        this.messageSender = messageSender;
    }

    /**
     * Checks if this handler supports the given command text.
     * It specifically handles the "/profile" command.
     *
     * @param commandText The text of the command received from the user.
     * @return true if the command text is "/profile", false otherwise.
     */
    @Override
    public boolean supports(String commandText) {
        return "/profile".equals(commandText);
    }

    /**
     * Processes the '/profile' command.
     * It fetches the formatted profile message from {@link UserServiceInt} and sends it to the user.
     *
     * @param absSender The {@link AbsSender} instance for sending Telegram responses.
     * @param update The {@link Update} object containing the command message.
     */
    @Override
    public void handle(AbsSender absSender, Update update) {
        long chatId = update.getMessage().getChatId();

        String message = userServiceInt.getProfileMessage(chatId);
        messageSender.sendMessage(absSender, chatId, message);
        logger.info("Replied to user {} with /profile command.", chatId);
    }
}