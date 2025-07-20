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
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;


/**
 * Handles callback queries related to calorie calculation agreement.
 * This includes "AGREE_CALCULATE" and "DISAGREE_CALCULATE" from initial prompts,
 * as well as choices for input method: "USE_PROFILE_DATA_CALCULATE" or "START_MANUAL_CALC_INPUT".
 * Based on the user's choice, it either initiates the calorie input flow (manual or from profile)
 * or sends a refusal message.
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
     * It supports "AGREE_CALCULATE", "DISAGREE_CALCULATE", "USE_PROFILE_DATA_CALCULATE",
     * and "START_MANUAL_CALC_INPUT" callback data.
     *
     * @param callbackData The data string from the callback query.
     * @return true if the data matches the supported agreement or input method callbacks, false otherwise.
     */
    @Override
    public boolean supports(String callbackData) {
        return "AGREE_CALCULATE".equals(callbackData) || "DISAGREE_CALCULATE".equals(callbackData)
                || "USE_PROFILE_DATA_CALCULATE".equals(callbackData)||
                "START_MANUAL_CALC_INPUT".equals(callbackData);
    }

    /**
     * Processes the calorie agreement or input method callback.
     * - If the user agrees to calculate:
     * - If their profile is complete, it offers options (manual or profile data).
     * - If their profile is incomplete, it directly starts the manual input flow.
     * - If the user disagrees, it sends a localized refusal message.
     * - If the user chooses to use profile data, it calculates from their profile.
     * - If the user explicitly chooses manual input, it starts the manual input flow.
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
            if (userServiceInt.isUserProfileComplete(user)) {
                sendCalorieInputOption(absSender, chatId, user);
                logger.info("User {} agreed to calculate calories. Profile is complete, offering input options.", chatId);
            } else {
                calorieCalculationFlowService.startCalorieInputFlow(absSender, chatId);
                logger.info("User {} agreed to calculate calories. Profile is incomplete, starting manual input directly.", chatId);
            }
        } else if ("DISAGREE_CALCULATE".equals(data)) {
            messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(language, "terms.of.use.disagree_message"));
            logger.info("User {} disagreed to calculate calories via callback.", chatId);
        }
        else if ("USE_PROFILE_DATA_CALCULATE".equals(data)) {
             if (userServiceInt.isUserProfileComplete(user)) {
                calorieCalculationFlowService.calculateFromProfile(absSender, chatId, user.getUPD());
                logger.info("User {} chose to use profile data for calorie calculation.", chatId);
            } else {
                messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(language, "error.profile_not_complete_for_auto_calc_manual_prompt"));
                calorieCalculationFlowService.startCalorieInputFlow(absSender, chatId);
                logger.warn("User {} tried to use incomplete profile for calorie calculation. Initiating manual input.", chatId);
            }
        } else if ("START_MANUAL_CALC_INPUT".equals(data)) {
            calorieCalculationFlowService.startCalorieInputFlow(absSender, chatId);
            logger.info("User {} chose manual input for calorie calculation.", chatId);
        }
    }

    /**
     * Sends a message to the user offering options for calorie input: manual or from profile.
     * The "Use Profile Data" option is only shown if the user's profile is complete.
     *
     * @param absSender The {@link AbsSender} instance.
     * @param chatId The chat ID of the user.
     * @param user The {@link User} object, used for localization and profile data check.
     */
    private void sendCalorieInputOption(AbsSender absSender, Long chatId, User user) {
        Language lang = user.getLanguage();

        String questionText = localizationService.getTranslation(lang, "question.calorie_input_method");

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(questionText);

        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        InlineKeyboardButton manualInputButton = new InlineKeyboardButton();
        manualInputButton.setText(localizationService.getTranslation(lang, "button.manual_input_calc"));
        manualInputButton.setCallbackData("START_MANUAL_CALC_INPUT");   buttons.add(List.of(manualInputButton));

        if (userServiceInt.isUserProfileComplete(user)) {
            InlineKeyboardButton useProfileDataButton = new InlineKeyboardButton();
            useProfileDataButton.setText(localizationService.getTranslation(lang, "button.use_profile_data_calc"));
            useProfileDataButton.setCallbackData("USE_PROFILE_DATA_CALCULATE");
            buttons.add(List.of(useProfileDataButton));
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(buttons);
        message.setReplyMarkup(markup);

        messageSender.sendMessage(absSender, message);
    }
}