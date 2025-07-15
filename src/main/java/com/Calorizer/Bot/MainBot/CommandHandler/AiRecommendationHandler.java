package com.Calorizer.Bot.MainBot.CommandHandler;
import com.Calorizer.Bot.MainBot.CallbackCallback.CallbackHandler;
import com.Calorizer.Bot.Model.Enum.Language;
import com.Calorizer.Bot.Model.User;
import com.Calorizer.Bot.Service.Interface.UserServiceInt;
import com.Calorizer.Bot.Service.LocalizationService;
import com.Calorizer.Bot.Service.NutritionRecommendationService;
import com.Calorizer.Bot.Service.MessageSender;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Handles the "/recommendation_ai" command and subsequent callback queries for AI-generated nutrition recommendations.
 * This class acts as both a {@link CommandHandler} and a {@link CallbackHandler},
 * managing the flow from initial command to user's selection of recommendation duration,
 * and finally triggering the AI recommendation generation.
 */
@Component
public class AiRecommendationHandler implements CommandHandler, CallbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(AiRecommendationHandler.class);

    private final NutritionRecommendationService nutritionRecommendationService;
    private final UserServiceInt userService;
    private final LocalizationService localizationService;
    private final MessageSender messageSender;

    private static final String AI_RECOMMENDATION_CALLBACK_PREFIX = "AI_REC_";

    private final Map<Long, String> userStates = new ConcurrentHashMap<>();

    /**
     * Constructor for dependency injection.
     *
     * @param nutritionRecommendationService Service for generating AI nutrition recommendations.
     * @param userService Service for user-related data operations.
     * @param localizationService Service for retrieving localized messages.
     * @param messageSender Service for sending messages to Telegram.
     */
    public AiRecommendationHandler(NutritionRecommendationService nutritionRecommendationService,
                                   UserServiceInt userService,
                                   LocalizationService localizationService,
                                   MessageSender messageSender) {
        this.nutritionRecommendationService = nutritionRecommendationService;
        this.userService = userService;
        this.localizationService = localizationService;
        this.messageSender = messageSender;
    }

    /**
     * Determines if this handler supports the given command text or callback data.
     * It supports:
     * <ul>
     * <li>The exact command "/recommendation_ai"</li>
     * <li>Any callback data starting with {@code AI_RECOMMENDATION_CALLBACK_PREFIX}</li>
     * <li>Localized button texts for the "AI Recommendation" command across various languages.</li>
     * </ul>
     *
     * @param commandText The incoming command text or callback data string.
     * @return {@code true} if the input is supported by this handler, {@code false} otherwise.
     */
    @Override
    public boolean supports(String commandText) {
        if (commandText.startsWith("/")) {
            return "/recommendation_ai".equals(commandText);
        }

        if (commandText.startsWith(AI_RECOMMENDATION_CALLBACK_PREFIX)) {
            return true;
        }
        return localizationService.getTranslation(Language.English, "button.command.recommendation_ai").equals(commandText) ||
                localizationService.getTranslation(Language.Russian, "button.command.recommendation_ai").equals(commandText) ||
                localizationService.getTranslation(Language.Ukrainian, "button.command.recommendation_ai").equals(commandText) ||
                localizationService.getTranslation(Language.German, "button.command.recommendation_ai").equals(commandText);
    }

    /**
     * Handles incoming {@link Update} objects, processing either text commands or callback queries
     * related to AI nutrition recommendations.
     * <p>
     * If a command (or localized button text) for AI recommendation is received, it sends an initial
     * message asking for the recommendation duration with inline buttons.
     * If a callback query with a duration prefix is received, it triggers the AI recommendation
     * generation and sends the result to the user.
     * </p>
     *
     * @param absSender The {@link AbsSender} instance, used to execute Telegram API methods.
     * @param update    The {@link Update} object containing the incoming message or callback query.
     */
    @Override
    public void handle(AbsSender absSender, Update update) {
        long chatId;

        if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
        } else {
            logger.warn("Received Update without message or callback query in AiRecommendationHandler. Update: {}", update);
            return;
        }

        User user = userService.getOrCreateUser(chatId);

        String receivedText = null;

        if (update.hasMessage() && update.getMessage().hasText()) {
            receivedText = update.getMessage().getText();

            if ("/recommendation_ai".equals(receivedText) ||
                    localizationService.getTranslation(user.getLanguage(), "button.command.recommendation_ai").equals(receivedText)) {
                sendInitialRecommendationRequest(absSender, chatId, user);
                logger.info("Initial AI recommendation request for user {}: {}", chatId, receivedText);
            } else {
                logger.warn("AiRecommendationHandler received an unhandled text message from user {}: {}", chatId, receivedText);
                messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(user.getLanguage(), "error.unexpected_input"));
            }

        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();

            if (callbackData.startsWith(AI_RECOMMENDATION_CALLBACK_PREFIX)) {
                String duration = callbackData.substring(AI_RECOMMENDATION_CALLBACK_PREFIX.length());
                requestAndSendAiRecommendation(absSender, chatId, user, duration, null);
                logger.info("Duration selection callback for user {}: {}", chatId, duration);
            } else {
                logger.warn("AiRecommendationHandler received unexpected callback query for user {}: {}", chatId, callbackData);
                messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(user.getLanguage(), "error.unexpected_callback"));
            }
        } else {
            logger.warn("AiRecommendationHandler received unsupported Update type for user {}. Update: {}", chatId, update);
            messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(user.getLanguage(), "error.unsupported_message_type"));
        }
    }

    /**
     * Sends an initial message to the user asking them to select the duration for the AI recommendation.
     * This message includes inline buttons for "Day" and "Week".
     *
     * @param absSender The {@link AbsSender} instance for sending messages.
     * @param chatId    The chat ID of the user.
     * @param user      The {@link User} object, used for localization of button texts.
     */
    private void sendInitialRecommendationRequest(AbsSender absSender, Long chatId, User user) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(localizationService.getTranslation(user.getLanguage(), "question.recommendation.duration"));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton dayButton = new InlineKeyboardButton();
        dayButton.setText(localizationService.getTranslation(user.getLanguage(), "button.duration.day"));
        dayButton.setCallbackData(AI_RECOMMENDATION_CALLBACK_PREFIX + "day");

        InlineKeyboardButton weekButton = new InlineKeyboardButton();
        weekButton.setText(localizationService.getTranslation(user.getLanguage(), "button.duration.week"));
        weekButton.setCallbackData(AI_RECOMMENDATION_CALLBACK_PREFIX + "week");

        rows.add(List.of(dayButton, weekButton));

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        messageSender.sendMessage(absSender, message);
    }

    /**
     * Requests a nutrition recommendation from the {@link NutritionRecommendationService} (AI)
     * and sends the generated text back to the user.
     * It subscribes to the {@code Mono<String>} returned by the service, handling both
     * successful responses and errors during AI generation.
     *
     * @param absSender          The {@link AbsSender} instance for sending messages.
     * @param chatId             The chat ID of the user.
     * @param user               The {@link User} object, used for profile data and error message localization.
     * @param duration           The selected duration for the recommendation (e.g., "day", "week").
     * @param additionalRequirements Any additional requirements for the AI, can be {@code null}.
     */
    private void requestAndSendAiRecommendation(AbsSender absSender, Long chatId, User user, String duration, String additionalRequirements) {
        nutritionRecommendationService.getNutritionRecommendation(user, duration, additionalRequirements)
                .subscribe(
                        recommendationText -> {
                            messageSender.sendMessage(absSender, chatId, recommendationText);
                            logger.info("AI recommendation sent to user {}", chatId);
                        },
                        error -> {
                            logger.error("Error getting AI recommendation for user {}: {}", chatId, error.getMessage());
                            messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(user.getLanguage(), "error.ai_generation_failed"));
                        }
                );
    }
}