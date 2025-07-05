package com.Calorizer.Bot.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Service responsible for sending messages to Telegram users.
 * It encapsulates the logic for interacting with the Telegram Bot API's message sending mechanism,
 * providing overloaded methods for convenience and robust error handling.
 */
@Service
public class MessageSender {

    private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);

    /**
     * Sends a simple text message to a specified chat ID.
     * This is a convenient overload for sending basic messages.
     *
     * @param absSender The {@link AbsSender} instance, which is the bot itself, used to execute API methods.
     * @param chatId The Telegram chat ID to send the message to.
     * @param text The text content of the message.
     */
    public void sendMessage(AbsSender absSender, Long chatId, String text) {
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        try {
            absSender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending text message to chat {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Sends a pre-configured {@link SendMessage} object to a user.
     * This overload is useful when the message object contains additional configurations
     * like reply markups, parse modes, or disables web page preview.
     *
     * @param absSender The {@link AbsSender} instance.
     * @param message The {@link SendMessage} object fully configured for sending.
     */
    public void sendMessage(AbsSender absSender, SendMessage message) {
        try {
            absSender.execute(message);
        } catch (TelegramApiException e) {
            String chatId = message.getChatId() != null ? message.getChatId() : "N/A";
            logger.error("Error sending configured message to chat {}: {}", chatId, e.getMessage());
        }
    }
}