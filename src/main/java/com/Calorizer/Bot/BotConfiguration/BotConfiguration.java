package com.Calorizer.Bot.BotConfiguration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
/**
 * Configuration class for Telegram bot properties.
 * It loads bot name and token from 'application.properties'.
 */
@Configuration
@Setter
@Getter
@PropertySource("application.properties")
public class BotConfiguration {
    /**
     * The username of the Telegram bot, configured in application.properties.
     */
    @Value("${bot.name}")
    private String botName;
    /**
     * The token for the Telegram bot, configured in application.properties.
     */
    @Value("${bot.token}")
    private String botToken;
}
