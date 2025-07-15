package com.Calorizer.Bot.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for sending messages to Telegram users.
 * It encapsulates the logic for interacting with the Telegram Bot API's message sending mechanism,
 * providing overloaded methods for convenience and robust error handling.
 * This service also handles splitting long messages into multiple parts to conform to Telegram's limits.
 */
@Service
public class MessageSender {

    private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);
    // Maximum allowed length for a single Telegram message (4096 characters).
    private static final int MAX_MESSAGE_LENGTH = 4096;
    // Delay between sending consecutive parts of a long message to avoid hitting Telegram's flood limits.
    private static final long MESSAGE_SEND_DELAY_MS = 50;

    /**
     * Sends a simple text message to a specified chat ID.
     * This method automatically handles texts longer than {@link #MAX_MESSAGE_LENGTH}
     * by splitting them into multiple messages and sending them sequentially with a small delay.
     *
     * @param absSender The {@link AbsSender} instance, which is the bot itself, used to execute API methods.
     * @param chatId    The Telegram chat ID to send the message to.
     * @param text      The text content of the message.
     */
    public void sendMessage(AbsSender absSender, Long chatId, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        if (text.length() <= MAX_MESSAGE_LENGTH) {
            SendMessage message = new SendMessage(String.valueOf(chatId), text);
            sendMessage(absSender, message);
        } else {

            List<String> parts = splitMessage(text, MAX_MESSAGE_LENGTH);
            for (int i = 0; i < parts.size(); i++) {
                String part = parts.get(i);
                SendMessage message = new SendMessage(String.valueOf(chatId), part);
                sendMessage(absSender, message);

                if (i < parts.size() - 1) {
                    try {
                        Thread.sleep(MESSAGE_SEND_DELAY_MS);
                    } catch (InterruptedException e) {
                        logger.warn("Thread interrupted while sending message parts for chat {}: {}", chatId, e.getMessage());
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    /**
     * Sends a pre-configured {@link SendMessage} object to a user.
     * This overload is useful when the message object contains additional configurations
     * like reply markups, inline keyboards, parse modes, or disables web page preview.
     * It handles {@link TelegramApiException} internally, logging the error.
     *
     * @param absSender The {@link AbsSender} instance, which is the bot itself.
     * @param message   The {@link SendMessage} object fully configured for sending.
     */
    public void sendMessage(AbsSender absSender, SendMessage message) {
        try {
            absSender.execute(message);
        } catch (TelegramApiException e) {
            String chatId = message.getChatId() != null ? message.getChatId() : "N/A";
            logger.error("Error sending configured message to chat {}: {}", chatId, e.getMessage());
        }
    }
    /**
     * Helper method to split a long string into smaller parts, trying to break at natural points
     * like newlines or spaces to avoid cutting words in the middle. It prioritizes newlines,
     * then spaces, and finally cuts if no natural break is found within a reasonable distance.
     *
     * @param text      The input string to split.
     * @param maxLength The maximum length for each part (excluding any added characters for splitting logic).
     * @return A list of strings, each not exceeding maxLength, representing the split parts of the original text.
     */
    private List<String> splitMessage(String text, int maxLength) {
        List<String> parts = new ArrayList<>();
        int currentPos = 0;
        while (currentPos < text.length()) {
            int endIndex = Math.min(currentPos + maxLength, text.length());
            String part = text.substring(currentPos, endIndex);

            if (endIndex < text.length()) {
                int lastNewLine = part.lastIndexOf('\n');
                int lastSpace = part.lastIndexOf(' ');

                if (lastNewLine > -1 && (part.length() - lastNewLine) < 200) {
                    endIndex = currentPos + lastNewLine + 1;
                    part = text.substring(currentPos, endIndex);
                } else if (lastSpace > -1 && (part.length() - lastSpace) < 200) {
                    endIndex = currentPos + lastSpace + 1;
                    part = text.substring(currentPos, endIndex);
                }
            }
            parts.add(part);
            currentPos = endIndex;
        }
        return parts;
    }
}