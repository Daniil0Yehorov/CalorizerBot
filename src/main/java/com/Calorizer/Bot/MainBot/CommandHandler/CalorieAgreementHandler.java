package com.Calorizer.Bot.MainBot.CommandHandler;
import com.Calorizer.Bot.Model.Enum.Language;
import com.Calorizer.Bot.Model.User;
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
import java.util.List;

/**
 * Handles the '/calculatecalorieforday' command and its localized button equivalents.
 * This handler is responsible for presenting the user with terms of use and data safety information
 * before proceeding with calorie calculation. It provides inline buttons for the user to
 * explicitly agree or disagree with these terms.
 */
@Component
public class CalorieAgreementHandler implements CommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(CalorieAgreementHandler.class);

    private final UserServiceInt userServiceInt;
    private final LocalizationService localizationService;
    private final MessageSender messageSender;

    /**
     * Constructor for dependency injection. Spring automatically provides instances of the required services.
     *
     * @param userServiceInt      Service for user-related operations, such as retrieving or creating user profiles.
     * @param localizationService Service for retrieving localized messages based on the user's language.
     * @param messageSender       Service for sending messages to Telegram users.
     */
    public CalorieAgreementHandler(UserServiceInt userServiceInt,
                                   LocalizationService localizationService,
                                   MessageSender messageSender) {
        this.userServiceInt = userServiceInt;
        this.localizationService = localizationService;
        this.messageSender = messageSender;
    }

    /**
     * Checks if this handler supports the given command text or its localized button equivalent.
     * It specifically handles the "/calculatecalorieforday" command and its translations
     * found in localization files for various languages.
     *
     * @param commandText The incoming command text or button text from the user.
     * @return {@code true} if the command text matches "/calculatecalorieforday" or its localized equivalents,
     * {@code false} otherwise.
     */
    @Override
    public boolean supports(String commandText) {
        if ("/calculatecalorieforday".equals(commandText)) {
            return true;
        }
        return localizationService.getTranslation(Language.English, "button.command.calculatecalorieforday").equals(commandText) ||
                localizationService.getTranslation(Language.Russian, "button.command.calculatecalorieforday").equals(commandText) ||
                localizationService.getTranslation(Language.Ukrainian, "button.command.calculatecalorieforday").equals(commandText) ||
                localizationService.getTranslation(Language.German, "button.command.calculatecalorieforday").equals(commandText);
    }

    /**
     * Handles the '/calculatecalorieforday' command.
     * It initiates the process by sending the calorie agreement terms to the user.
     *
     * @param absSender The {@link AbsSender} instance for sending Telegram responses.
     * @param update The {@link Update} object containing the command message.
     */
    @Override
    public void handle(AbsSender absSender, Update update) {
        long chatId = update.getMessage().getChatId();
        sendCalorieAgreementStep(absSender, chatId);
        logger.info("Sent calorie agreement step to user {} via command.", chatId);
    }

    /**
     * Constructs and sends the message containing terms of use and agreement buttons.
     *
     * @param absSender The {@link AbsSender} instance.
     * @param chatId The chat ID of the user to send the message to.
     */
    private void sendCalorieAgreementStep(AbsSender absSender, Long chatId) {
        User user = userServiceInt.getOrCreateUser(chatId);
        Language language = user.getLanguage();

        String termsTitle = localizationService.getTranslation(language, "terms.of.use.title");
        String disclaimer = localizationService.getTranslation(language, "terms.of.use.disclaimer");
        String dataSafety = localizationService.getTranslation(language, "terms.of.use.data_safety");

        String termsText = termsTitle + "\n\n" + disclaimer + "\n" + dataSafety;

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(termsText);

        InlineKeyboardButton agreeButton = new InlineKeyboardButton();
        agreeButton.setText(localizationService.getTranslation(language, "terms.of.use.agree_button"));
        agreeButton.setCallbackData("AGREE_CALCULATE");

        InlineKeyboardButton disagreeButton = new InlineKeyboardButton();
        disagreeButton.setText(localizationService.getTranslation(language, "terms.of.use.disagree_button"));
        disagreeButton.setCallbackData("DISAGREE_CALCULATE");

        List<List<InlineKeyboardButton>> buttons = List.of(List.of(disagreeButton, agreeButton));
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(buttons);
        message.setReplyMarkup(markup);

        messageSender.sendMessage(absSender, message);
    }
}