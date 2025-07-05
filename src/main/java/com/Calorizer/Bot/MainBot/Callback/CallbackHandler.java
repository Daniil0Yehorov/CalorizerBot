package com.Calorizer.Bot.MainBot.Callback;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

/**
 * Interface for handling Telegram bot {@link Update}s that contain a {@link org.telegram.telegrambots.meta.api.objects.CallbackQuery}.
 * Implementations of this interface define specific logic for different types of callback data.
 */
public interface CallbackHandler {
    /**
     * Checks if this handler supports the given callback data.
     *
     * @param callbackData The data string received from a callback query.
     * @return true if this handler can process the callback data, false otherwise.
     */
    boolean supports(String callbackData);

    /**
     * Handles the callback query.
     *
     * @param absSender The {@link AbsSender} instance used to send responses back to Telegram.
     * @param update The {@link Update} object containing the callback query details.
     */
    void handle(AbsSender absSender, Update update);
}
