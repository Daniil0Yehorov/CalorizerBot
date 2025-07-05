package com.Calorizer.Bot.MainBot.Handler;


import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

/**
 * Interface for handling Telegram bot {@link Update}s that contain a command (messages starting with '/').
 * Implementations of this interface define specific logic for different bot commands.
 */
public interface CommandHandler {
    boolean supports(String commandText);
    void handle(AbsSender absSender, Update update);
}