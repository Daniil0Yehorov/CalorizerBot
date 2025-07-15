package com.Calorizer.Bot.MainBot;

import com.Calorizer.Bot.BotConfiguration.BotConfiguration;
import com.Calorizer.Bot.MainBot.CallbackCallback.CallbackHandler;
import com.Calorizer.Bot.MainBot.CommandHandler.CommandHandler;
import com.Calorizer.Bot.Service.CalorieCalculationFlowService;
import com.Calorizer.Bot.Service.ProfileUpdateDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Optional;

/**
 * The main Telegram bot class, extending {@link TelegramLongPollingBot}.
 * This class is responsible for receiving updates from Telegram and delegating
 * them to appropriate command handlers, callback handlers, or flow services.
 */
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    private final BotConfiguration botConfiguration;
    private final CalorieCalculationFlowService calorieCalculationFlowService;
    private final List<CommandHandler> commandHandlers;
    private final List<CallbackHandler> callbackHandlers;
    private final ProfileUpdateDataService profileUpdateDataService;

    /**
     * Constructor for dependency injection.
     * Spring automatically injects the {@link BotConfiguration} and instances of
     * all discovered {@link CommandHandler} and {@link CallbackHandler} beans into their respective lists.
     * It also injects the specific flow services responsible for multi-step interactions.
     *
     * @param botConfiguration              Configuration properties for the bot (name, token).
     * @param calorieCalculationFlowService Service to manage multi-step user input for calorie calculation.
     * @param commandHandlers               A list of all available {@link CommandHandler} implementations.
     * @param callbackHandlers              A list of all available {@link CallbackHandler} implementations.
     * @param profileUpdateDataService      Service to manage multi-step user input for profile data updates.
     */
    public TelegramBot(BotConfiguration botConfiguration,
                       CalorieCalculationFlowService calorieCalculationFlowService,
                       List<CommandHandler> commandHandlers,
                       List<CallbackHandler> callbackHandlers, ProfileUpdateDataService profileUpdateDataService) {
        super(botConfiguration.getBotToken());
        this.botConfiguration = botConfiguration;
        this.calorieCalculationFlowService = calorieCalculationFlowService;
        this.commandHandlers = commandHandlers;
        this.callbackHandlers = callbackHandlers;
        this.profileUpdateDataService = profileUpdateDataService;
        logger.info("TelegramBot initialized with {} command handlers and {} callback handlers.",
                commandHandlers.size(), callbackHandlers.size());
    }

    /**
     * Returns the username of the bot, as configured.
     *
     * @return The bot's username.
     */
    @Override
    public String getBotUsername() {
        return botConfiguration.getBotName();
    }

    /**
     * Returns the token of the bot, as configured.
     * This method is inherited from AbsSender and is required by TelegramBots library.
     *
     * @return The bot's token.
     */
    @Override
    public String getBotToken() {
        return botConfiguration.getBotToken();
    }

    /**
     * This is the primary entry point for all incoming updates from the Telegram Bot API.
     * It intelligently dispatches the {@link Update} to the appropriate processing logic:
     * <ol>
     * <li>If the update contains a text message:
     * <ul>
     * <li>Checks if the user is in an active calorie calculation input flow.</li>
     * <li>Checks if the user is in an active profile update input flow.</li>
     * <li>If not in a flow, attempts to find a suitable {@link CommandHandler} for the message (if it starts with '/').</li>
     * <li>If no specific command handler is found or the message is not a command, it's typically handled by a default/unknown command handler.</li>
     * </ul>
     * </li>
     * <li>If the update contains a callback query (from an inline keyboard button):
     * <ul>
     * <li>Checks if the callback is related to the profile update service (e.g., attribute selection).</li>
     * <li>If not, attempts to find a suitable {@link CallbackHandler} for the callback data.</li>
     * <li>Logs unhandled callback queries.</li>
     * </ul>
     * </li>
     * </ol>
     *
     * @param update The {@link Update} object received from Telegram, containing various types of data.
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();

            if (calorieCalculationFlowService.isInCalorieInputFlow(chatId)) {
                calorieCalculationFlowService.handleCalorieInputStep(this, chatId, messageText);
                return;
            }
            if (profileUpdateDataService.isInProfileUpdateFlow(chatId)) {
                profileUpdateDataService.handleProfileInputStep(this, chatId, messageText);
                return;
            }

                Optional<CommandHandler> suitableHandler = commandHandlers.stream()
                        .filter(handler -> handler.supports(messageText))
                        .findFirst();

                if (suitableHandler.isPresent()) {
                    suitableHandler.get().handle(this, update);
                } else {
                    commandHandlers.stream()
                            .filter(handler -> handler.getClass().getSimpleName().equals("UnknownCommandHandler"))
                            .findFirst()
                            .ifPresent(handler -> handler.handle(this, update));
                }
        }
        else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (profileUpdateDataService.isInProfileUpdateFlow(chatId) &&
                    (callbackData.startsWith("UPDATE_") || "UPDATE_PROFILE_DONE".equals(callbackData))) {
                profileUpdateDataService.handleAttributeSelectionCallback(this, chatId, callbackData);
                return;
            }

            Optional<CallbackHandler> suitableHandler = callbackHandlers.stream()
                    .filter(handler -> handler.supports(callbackData))
                    .findFirst();

            if (suitableHandler.isPresent()) {
                suitableHandler.get().handle(this, update);
                 return;
            } else {
                logger.warn("Received unhandled callback query from user {}: {}", chatId, callbackData);
            }
        }
    }
}