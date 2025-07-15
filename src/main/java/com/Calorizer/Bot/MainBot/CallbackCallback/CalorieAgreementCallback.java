package com.Calorizer.Bot.MainBot.CallbackCallback;

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
import org.telegram.telegrambots.meta.api.objects.Update;


/**
 * Handles callback queries related to calorie calculation agreement (e.g., AGREE_CALCULATE, DISAGREE_CALCULATE).
 * Based on user's choice, it either initiates the calorie input flow or sends a refusal message.
 */
@Component
public class CalorieAgreementCallback implements CallbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(CalorieAgreementCallback.class);

    private final UserServiceInt userServiceInt;
    private final LocalizationService localizationService;
    private final MessageSender messageSender;
    private final CalorieCalculationFlowService calorieCalculationFlowService;

    /**
     * Constructor for dependency injection. Spring automatically provides instances of the required services.
     *
     * @param userServiceInt Service for user-related operations.
     * @param localizationService Service for retrieving localized messages.
     * @param messageSender Service for sending messages to Telegram.
     * @param calorieCalculationFlowService Service managing the multi-step calorie input process.
     */
    public CalorieAgreementCallback(UserServiceInt userServiceInt,
                                    LocalizationService localizationService,
                                    MessageSender messageSender,
                                    CalorieCalculationFlowService calorieCalculationFlowService) {
        this.userServiceInt = userServiceInt;
        this.localizationService = localizationService;
        this.messageSender = messageSender;
        this.calorieCalculationFlowService = calorieCalculationFlowService;
    }

    /**
     * Determines if this handler can process the given callback data.
     * It supports "AGREE_CALCULATE" and "DISAGREE_CALCULATE" callback data.
     *
     * @param callbackData The data string from the callback query.
     * @return true if the data matches the supported agreement callbacks, false otherwise.
     */
    @Override
    public boolean supports(String callbackData) {
        return "AGREE_CALCULATE".equals(callbackData) || "DISAGREE_CALCULATE".equals(callbackData);
    }

    /**
     * Processes the calorie agreement callback.
     * If the user agrees, it starts the calorie input flow.
     * If the user disagrees, it sends a localized refusal message.
     *
     * @param absSender The {@link AbsSender} instance for sending Telegram responses.
     * @param update The {@link Update} object containing the callback query.
     */
    @Override
    public void handle(AbsSender absSender, Update update) {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String data = update.getCallbackQuery().getData();
        User user = userServiceInt.getOrCreateUser(chatId);
        Language language = user.getLanguage();

        if ("AGREE_CALCULATE".equals(data)) {
            calorieCalculationFlowService.startCalorieInputFlow(absSender, chatId);
            logger.info("User {} agreed to calculate calories via callback, starting input flow.", chatId);
        } else if ("DISAGREE_CALCULATE".equals(data)) {
            messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(language, "terms.of.use.disagree_message"));
            logger.info("User {} disagreed to calculate calories via callback.", chatId);
        }
    }
}