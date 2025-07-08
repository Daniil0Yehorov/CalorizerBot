package com.Calorizer.Bot.MainBot.Callback;

import com.Calorizer.Bot.Model.Enum.Language;
import com.Calorizer.Bot.Model.User;
import com.Calorizer.Bot.Service.Interface.UserServiceInt;
import com.Calorizer.Bot.Service.LocalizationService;
import com.Calorizer.Bot.Service.MessageSender;
import com.Calorizer.Bot.Service.ProfileUpdateDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles callback queries related to profile update actions initiated by the user.
 * This includes responses to the initial "Do you want to update profile?" question,
 * and choices on how to update (all attributes or one by one).
 * It interacts with {@link ProfileUpdateDataService} to manage the actual update flow.
 */
@Component
public class ProfileCallback implements CallbackHandler{

    private static final Logger logger = LoggerFactory.getLogger(ProfileCallback.class);

    private final UserServiceInt userServiceInt;
    private final LocalizationService localizationService;
    private final MessageSender messageSender;
    private final ProfileUpdateDataService profileUpdateDataService;

    /**
     * Constructs a new {@code ProfileCallback} instance, injecting necessary services.
     *
     * @param userServiceInt         Service for user-related data operations.
     * @param localizationService    Service for retrieving localized strings.
     * @param messageSender          Service for sending messages back to the user.
     * @param profileUpdateDataService Service for managing the multi-step profile data update flow.
     */
    public ProfileCallback(UserServiceInt userServiceInt, LocalizationService localizationService, MessageSender messageSender, ProfileUpdateDataService profileUpdateDataService) {
        this.userServiceInt = userServiceInt;
        this.localizationService = localizationService;
        this.messageSender = messageSender;
        this.profileUpdateDataService = profileUpdateDataService;
    }

    /**
     * Checks if this callback handler supports the given callback data.
     * It specifically supports callback data related to initiating or cancelling profile updates.
     *
     * @param callbackData The string data received from the inline keyboard button.
     * @return {@code true} if this handler can process the callback data, {@code false} otherwise.
     */
    @Override
    public boolean supports(String callbackData) {
        return "PROFILE_DATA_UPDATE_YES".equals(callbackData) ||
                "PROFILE_DATA_UPDATE_NO".equals(callbackData) ||
                "PROFILE_UPDATE_ALL".equals(callbackData) ||
                "PROFILE_UPDATE_ONE_BY_ONE".equals(callbackData);
    }

    /**
     * Handles the incoming callback query related to profile update.
     * <p>
     * It performs the following steps:
     * <ol>
     * <li>Retrieves user information and their preferred language.</li>
     * <li>Checks if the user has a paid account; if not, sends an access denied message and logs a warning.</li>
     * <li>Based on the callback data, it directs the flow:
     * <ul>
     * <li>If "YES", it presents options to update all attributes or one by one.</li>
     * <li>If "NO", it cancels the update process.</li>
     * <li>If "UPDATE_ALL", it initiates a full profile update flow via {@link ProfileUpdateDataService}.</li>
     * <li>If "UPDATE_ONE_BY_ONE", it initiates a single attribute update flow via {@link ProfileUpdateDataService}.</li>
     * </ul>
     * </li>
     * <li>Logs the user's action.</li>
     * </ol>
     * </p>
     *
     * @param absSender The {@link AbsSender} instance for sending Telegram responses.
     * @param update    The {@link Update} object containing the callback query details.
     */
    @Override
    public void handle(AbsSender absSender, Update update) {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String data = update.getCallbackQuery().getData();
        User user = userServiceInt.getOrCreateUser(chatId);
        Language language = user.getLanguage();

        if (!user.isPayedAcc()) {
            messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(language, "profile.access_paid_only"));
            logger.warn("User {} (non-paid) tried to interact with profile update callbacks.", chatId);
            return;
        }
        if ("PROFILE_DATA_UPDATE_YES".equals(data)) {

            sendUpdateOptionsKeyboard(absSender, chatId, language);
            logger.info("User {} agreed to update profile, presenting options.", chatId);
        } else if ("PROFILE_DATA_UPDATE_NO".equals(data)) {

            messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(language, "message.profile.update.cancelled"));
            logger.info("User {} declined to update profile.", chatId);
        } else if ("PROFILE_UPDATE_ALL".equals(data)) {

            profileUpdateDataService.startProfileUpdateFlow(absSender, chatId, ProfileUpdateDataService.UpdateMode.ALL_ATTRIBUTES);
            logger.info("User {} chose to update all profile attributes.", chatId);
        } else if ("PROFILE_UPDATE_ONE_BY_ONE".equals(data)) {

            profileUpdateDataService.startProfileUpdateFlow(absSender, chatId, ProfileUpdateDataService.UpdateMode.SINGLE_ATTRIBUTE);
            logger.info("User {} chose to update profile attributes one by one.", chatId);
        }
    }

    /**
     * Sends an inline keyboard to the user, allowing them to choose how they want to update their profile:
     * either updating "all" attributes sequentially or "one by one" (selecting specific attributes).
     *
     * @param absSender The {@link AbsSender} instance for sending messages.
     * @param chatId    The chat ID of the user.
     * @param lang      The user's preferred {@link Language} for message localization.
     */
    private void sendUpdateOptionsKeyboard(AbsSender absSender, Long chatId, Language lang) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(localizationService.getTranslation(lang, "question.profile.update.how"));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton buttonUpdateAll = new InlineKeyboardButton();
        buttonUpdateAll.setText(localizationService.getTranslation(lang, "button.profile.update.all"));
        buttonUpdateAll.setCallbackData("PROFILE_UPDATE_ALL");

        InlineKeyboardButton buttonUpdateOneByOne = new InlineKeyboardButton();
        buttonUpdateOneByOne.setText(localizationService.getTranslation(lang, "button.profile.update.one_by_one"));
        buttonUpdateOneByOne.setCallbackData("PROFILE_UPDATE_ONE_BY_ONE");

        rows.add(List.of(buttonUpdateAll));
        rows.add(List.of(buttonUpdateOneByOne));

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        messageSender.sendMessage(absSender, message);
    }
}
