package com.Calorizer.Bot.MainBot.CommandHandler;
import com.Calorizer.Bot.MainBot.KeyboardFactory;
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

import java.text.MessageFormat;

/**
 * Handles the '/start' command.
 * This handler is responsible for the initial greeting to a user,
 * ensuring their profile exists (or creating a new one with a default language),
 * updating the bot's command list in the Telegram client to match the user's language,
 * and sending the main menu keyboard.
 */
@Component
public class StartCommandHandler implements CommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(StartCommandHandler.class);

    private final UserServiceInt userServiceInt;
    private final LocalizationService localizationService;
    private final MessageSender messageSender;
    private final TelegramBotCommandsUpdater commandsUpdater;
    private final KeyboardFactory keyboardFactory;

    /**
     * Constructor for dependency injection.
     *
     * @param userServiceInt Service for user-related operations (e.g., getting or creating user,
     * and managing user data in the database).
     * @param localizationService Service for retrieving localized messages based on the user's language.
     * @param messageSender Service for sending messages and other responses back to the Telegram client.
     * @param commandsUpdater Service responsible for updating bot commands in the Telegram UI,
     * which helps in localizing the bot's command menu.
     * @param keyboardFactory Factory for creating custom Telegram keyboards,
     * specifically the main menu keyboard in this context.
     */
    public StartCommandHandler(UserServiceInt userServiceInt,
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
     * This method performs the following actions:
     * 1. Retrieves or creates the user's profile in the database.
     * 2. Updates the bot's command list in the Telegram client to reflect the user's language.
     * 3. Constructs a personalized greeting message for the user.
     * 4. Creates and attaches the main menu {@link org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup}
     * to the greeting message, localized to the user's language.
     * 5. Sends the greeting message with the main menu keyboard to the user.
     *
     * @param absSender The {@link AbsSender} instance for sending Telegram responses.
     * @param update The {@link Update} object containing the command message received from the user.
     */
    @Override
    public void handle(AbsSender absSender, Update update) {
        long chatId = update.getMessage().getChatId();
        String username = update.getMessage().getChat().getFirstName();

        User user = userServiceInt.getOrCreateUser(chatId);

        commandsUpdater.updateCommands(absSender, chatId, user.getLanguage());

        String greetingTemplate = localizationService.getTranslation(user.getLanguage(), "greeting");
        String text = MessageFormat.format(greetingTemplate, username);

        SendMessage welcomeMessage = new SendMessage();
        welcomeMessage.setChatId(String.valueOf(chatId));
        welcomeMessage.setText(text);

        welcomeMessage.setReplyMarkup(keyboardFactory.createMainMenuKeyboard(user.getLanguage()));

        messageSender.sendMessage(absSender, welcomeMessage);
        logger.info("Replied to user {} with /start command and main menu keyboard.", username);
    }
}