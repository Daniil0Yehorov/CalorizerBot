package com.Calorizer.Bot.MainBot.Handler;

import com.Calorizer.Bot.Model.User;
import com.Calorizer.Bot.Service.Interface.UserServiceInt;
import com.Calorizer.Bot.Service.LocalizationService;
import com.Calorizer.Bot.Service.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the "/profile" command received from users.
 * This handler is responsible for retrieving and displaying the user's profile information.
 * It also manages access restrictions based on the user's payment status and
 * provides an option to update the profile for paid users.
 */
@Component
public class ProfileHandler implements CommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProfileHandler.class);

    private final UserServiceInt userServiceInt;
    private final MessageSender messageSender;
    private final LocalizationService localizationService;

    /**
     * Constructs a new {@code ProfileHandler} instance, injecting necessary services.
     *
     * @param userServiceInt      Service for user-related data operations, including profile retrieval and creation.
     * @param messageSender       Service for sending messages back to the user via Telegram API.
     * @param localizationService Service for retrieving localized strings based on user's language.
     */
    public ProfileHandler(UserServiceInt userServiceInt, MessageSender messageSender,LocalizationService localizationService) {
        this.userServiceInt = userServiceInt;
        this.messageSender = messageSender;
        this.localizationService=localizationService;
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
     * Processes the "/profile" command.
     * <p>
     * It performs the following steps:
     * <ol>
     * <li>Retrieves or creates the user based on the chat ID.</li>
     * <li>Fetches the formatted profile message from {@link UserServiceInt} (which handles paid account checks internally).</li>
     * <li>Sends the profile message to the user.</li>
     * <li>If the user has a paid account, it sends an inline keyboard offering options to update profile data.</li>
     * <li>Logs the interaction, including the user's paid status.</li>
     * </ol>
     * </p>
     *
     * @param absSender The {@link AbsSender} instance, used to send responses back to Telegram.
     * @param update    The {@link Update} object containing the command message details.
     */
    @Override
    public void handle(AbsSender absSender, Update update) {
        long chatId = update.getMessage().getChatId();
        User user = userServiceInt.getOrCreateUser(chatId);
        String message = userServiceInt.getProfileMessage(chatId);
        messageSender.sendMessage(absSender, chatId, message);

        if (user.isPayedAcc()) {
            sendUpdateProfileDataKeyboard(absSender,chatId);
        }
        logger.info("Replied to user {} with /profile command. Paid status: {}", chatId, user.isPayedAcc());
    }

    /**
     * Sends an inline keyboard to the user, prompting them if they want to update their profile data.
     * This keyboard includes "Yes" and "No" options, leading to different callback actions.
     * This method is only called for users with paid accounts.
     *
     * @param absSender The {@link AbsSender} instance for sending messages.
     * @param chatId    The chat ID of the user.
     */
    private void sendUpdateProfileDataKeyboard(AbsSender absSender, Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        User user = userServiceInt.getOrCreateUser(chatId);
        message.setText(localizationService.getTranslation(user.getLanguage(), "question.profile.update"));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton button_yes = new InlineKeyboardButton();
        button_yes.setText(localizationService.getTranslation(user.getLanguage(), "question.profile.update.yes"));
        button_yes.setCallbackData("PROFILE_DATA_UPDATE_YES");

        InlineKeyboardButton button_no = new InlineKeyboardButton();
        button_no.setText(localizationService.getTranslation(user.getLanguage(), "question.profile.update.no"));
        button_no.setCallbackData("PROFILE_DATA_UPDATE_NO");

        rows.add(List.of(button_yes,button_no));

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        messageSender.sendMessage(absSender, message);
    }

}