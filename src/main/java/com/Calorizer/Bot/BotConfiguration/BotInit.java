package com.Calorizer.Bot.BotConfiguration;

import com.Calorizer.Bot.MainBot.TelegramBot;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Component responsible for initializing and registering the Telegram bot.
 * This class ensures the bot starts running after the Spring application context is initialized.
 */
@Slf4j
@Component
public class BotInit {
    private final TelegramBot telegramBot;

    public BotInit(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    /**
     * Initializes the Telegram Bots API and registers the bot.
     */
    @PostConstruct
    public void init() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
