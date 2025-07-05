package com.Calorizer.Bot.MainBot.Handler;
import com.Calorizer.Bot.Model.Enum.Language;
import com.Calorizer.Bot.Model.User;
import com.Calorizer.Bot.Service.CalorieCalculationFlowService;
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
 * Handles the '/calculatecalorieforday' command.
 * This handler sends a message to the user outlining terms of use and data safety,
 * along with inline buttons for agreement or disagreement to proceed with calorie calculation.
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
     * @param userServiceInt Service for user-related operations.
     * @param localizationService Service for retrieving localized messages.
     * @param messageSender Service for sending messages to Telegram.
     */
    public CalorieAgreementHandler(UserServiceInt userServiceInt,
                                   LocalizationService localizationService,
                                   MessageSender messageSender) {
        this.userServiceInt = userServiceInt;
        this.localizationService = localizationService;
        this.messageSender = messageSender;
    }

    /**
     * Checks if this handler supports the given command text.
     * It specifically handles the "/calculatecalorieforday" command.
     *
     * @param commandText The text of the command received from the user.
     * @return true if the command text is "/calculatecalorieforday", false otherwise.
     */
    @Override
    public boolean supports(String commandText) {
        return "/calculatecalorieforday".equals(commandText);
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