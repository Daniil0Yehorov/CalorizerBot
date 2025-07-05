package com.Calorizer.Bot.MainBot;

import com.Calorizer.Bot.BotConfiguration.BotConfiguration;
import com.Calorizer.Bot.MainBot.Callback.CallbackHandler;
import com.Calorizer.Bot.MainBot.Handler.CommandHandler;
import com.Calorizer.Bot.Service.CalorieCalculationFlowService;
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

    /**
     * Constructor for dependency injection. Spring automatically injects
     * all registered {@link CommandHandler} and {@link CallbackHandler} beans into the respective lists.
     *
     * @param botConfiguration Configuration properties for the bot (name, token).
     * @param calorieCalculationFlowService Service to manage multi-step calorie input flow.
     * @param commandHandlers A list of all available command handlers.
     * @param callbackHandlers A list of all available callback handlers.
     */
    public TelegramBot(BotConfiguration botConfiguration,
                       CalorieCalculationFlowService calorieCalculationFlowService,
                       List<CommandHandler> commandHandlers,
                       List<CallbackHandler> callbackHandlers) {
        super(botConfiguration.getBotToken());
        this.botConfiguration = botConfiguration;
        this.calorieCalculationFlowService = calorieCalculationFlowService;
        this.commandHandlers = commandHandlers;
        this.callbackHandlers = callbackHandlers;
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
     * This method is called when an update is received from the Telegram Bot API.
     * It dispatches the update to the appropriate handler based on its type (message, callback query).
     *
     * @param update The {@link Update} object received from Telegram.
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

            if (messageText.startsWith("/")) {
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
            else {
                logger.warn("Received non-command, non-flow text from user {}: {}", chatId, messageText);
                commandHandlers.stream()
                        .filter(handler -> handler.getClass().getSimpleName().equals("UnknownCommandHandler"))
                        .findFirst()
                        .ifPresent(handler -> handler.handle(this, update));
            }

        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            Optional<CallbackHandler> suitableHandler = callbackHandlers.stream()
                    .filter(handler -> handler.supports(callbackData))
                    .findFirst();

            if (suitableHandler.isPresent()) {
                suitableHandler.get().handle(this, update);
            } else {
                logger.warn("Received unhandled callback query from user {}: {}", chatId, callbackData);
            }
        }
    }
}