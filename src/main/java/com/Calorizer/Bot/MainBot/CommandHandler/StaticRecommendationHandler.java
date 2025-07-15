package com.Calorizer.Bot.MainBot.CommandHandler;

import com.Calorizer.Bot.Model.Enum.Language;
import com.Calorizer.Bot.Model.Enum.MainGoal;
import com.Calorizer.Bot.Model.User;
import com.Calorizer.Bot.Model.UserPhysicalData;
import com.Calorizer.Bot.Service.Interface.UserServiceInt;
import com.Calorizer.Bot.Service.LocalizationService;
import com.Calorizer.Bot.Service.MessageSender;
import com.Calorizer.Bot.Service.TelegramBotCommandsUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

/**
 * Handles the "/recommendation_static" command.
 * This handler provides a static recommendation message to the user,
 * based on their pre-defined main goal (e.g., weight loss, maintenance, weight gain).
 * It requires the user's profile to have a specified main goal.
 */
@Component
public class StaticRecommendationHandler implements CommandHandler{

    private static final Logger logger = LoggerFactory.getLogger(StaticRecommendationHandler.class);

    private final UserServiceInt userServiceInt;
    private final LocalizationService localizationService;
    private final MessageSender messageSender;
    private final TelegramBotCommandsUpdater commandsUpdater;

    /**
     * Constructs a new StaticRecommendationHandler with necessary dependencies.
     * These dependencies are injected by Spring.
     *
     * @param userServiceInt Service for managing user-related data, like retrieving or creating user profiles.
     * @param localizationService Service for fetching localized messages based on the user's language.
     * @param messageSender Service for sending messages back to the user via the Telegram Bot API.
     * @param commandsUpdater Service responsible for updating the bot's command list in the Telegram UI.
     */
    public StaticRecommendationHandler(UserServiceInt userServiceInt,
                               LocalizationService localizationService,
                               MessageSender messageSender,
                               TelegramBotCommandsUpdater commandsUpdater) {
        this.userServiceInt = userServiceInt;
        this.localizationService = localizationService;
        this.messageSender = messageSender;
        this.commandsUpdater = commandsUpdater;
    }

    /**
     * Determines if this handler can process the given command text or localized button text.
     * It specifically supports the "/recommendation_static" command and its translations
     * found in localization files for various languages.
     *
     * @param commandText The incoming command text or button text from the user.
     * @return {@code true} if the command text matches "/recommendation_static" or its localized equivalents,
     * {@code false} otherwise.
     */
    @Override
    public boolean supports(String commandText) {
        if ("/recommendation_static".equals(commandText)) {
            return true;
        }
        return localizationService.getTranslation(Language.English, "button.command.recommendation_static").equals(commandText) ||
                localizationService.getTranslation(Language.Russian, "button.command.recommendation_static").equals(commandText) ||
                localizationService.getTranslation(Language.Ukrainian, "button.command.recommendation_static").equals(commandText) ||
                localizationService.getTranslation(Language.German, "button.command.recommendation_static").equals(commandText);
    }

    /**
     * Processes the "/recommendation_static" command.
     * It retrieves the user's main goal from their profile and sends a corresponding
     * static recommendation message. If the main goal is not set in the user's profile,
     * it prompts the user to complete their profile first.
     *
     * @param absSender The {@link AbsSender} instance, used to execute Telegram API methods.
     * @param update    The {@link Update} object containing the incoming message details.
     */
    @Override
    public void handle(AbsSender absSender, Update update) {
        long chatId = update.getMessage().getChatId();
        String username = update.getMessage().getChat().getFirstName();

        User user = userServiceInt.getOrCreateUser(chatId);

        commandsUpdater.updateCommands(absSender, chatId, user.getLanguage());

        String messageToSend;
        UserPhysicalData upd = user.getUPD();

        if (upd == null || upd.getMaingoal() == null) {
            messageToSend = localizationService.getTranslation(user.getLanguage(), "error.profile_not_complete_for_recommendations");
            logger.warn("User {} ({}) tried to get static recommendation, but profile (MainGoal) is not set.", username, chatId);
        } else {
            MainGoal userGoal = upd.getMaingoal();

            String goalNameKey = "enum.goal." + userGoal.name().toLowerCase();
            String translatedGoalName = localizationService.getTranslation(user.getLanguage(), goalNameKey);

            String recommendationKey = "enum.goal." + userGoal.name().toLowerCase() + ".recommendation_static";
            String recommendationText = localizationService.getTranslation(user.getLanguage(), recommendationKey);

            if (!recommendationText.isEmpty() && !recommendationText.equals(recommendationKey)) {
                messageToSend = translatedGoalName + "\n\n" + recommendationText;
            } else {
                logger.warn("Missing static recommendation translation for goal '{}' and language '{}'. Using fallback.", userGoal.name(), user.getLanguage());
                messageToSend = localizationService.getTranslation(user.getLanguage(), "recommendation.static.fallback");
            }
            logger.info("Replied to user {} ({}) with /recommendation_static command. Goal: {}", username, chatId, userGoal);
        }

        messageSender.sendMessage(absSender, chatId, messageToSend);
    }
}
