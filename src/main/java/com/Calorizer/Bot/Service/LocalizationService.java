package com.Calorizer.Bot.Service;

import com.Calorizer.Bot.Model.Enum.Language;
import com.Calorizer.Bot.Model.Enum.MainGoal;
import com.Calorizer.Bot.Model.Enum.PhysicalActivityLevel;
import com.Calorizer.Bot.Model.Enum.Sex;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.text.MessageFormat;

/**
 * Service responsible for managing all localized strings and bot commands for different languages.
 * It loads messages from resource bundles and provides methods to retrieve translations
 * for various types of content (general messages, enum values, calculation methods).
 */
@Service
public class LocalizationService {

    private final Map<Language, List<BotCommand>> localizedCommands = new HashMap<>();
    private final Map<Language, ResourceBundle> messagesBundles = new HashMap<>();
    /**
     * Initializes the localization service after the bean has been constructed.
     * This method loads all {@link ResourceBundle}s for each supported {@link Language}
     * and populates the map of localized bot commands.
     * This improved version iterates through languages, reducing code duplication.
     */
    @PostConstruct
    public void init() {
        for (Language lang : Language.values()) {
            ResourceBundle bundle = ResourceBundle.getBundle("messages/messages", lang.getLocale());
            messagesBundles.put(lang, bundle);

            List<BotCommand> commands = List.of(
                    new BotCommand("/start", getTranslation(lang, "command.start.description")),
                    new BotCommand("/profile", getTranslation(lang, "command.profile.description")),
                    new BotCommand("/changelanguage", getTranslation(lang, "command.changelanguage.description")),
                    new BotCommand("/calculatecalorieforday", getTranslation(lang, "command.calculatecalorieforday.description")),
                    new BotCommand("/recommendation_static", getTranslation(lang, "command.recommendation_static.description")),
                    new BotCommand("/recommendation_ai", getTranslation(lang, "command.recommendation_ai.description"))
                    );

            localizedCommands.put(lang, commands);
        }
    }

    /**
     * Retrieves a localized string for a given key and language.
     * Supports message formatting with arguments (e.g., "Hello, {0}!").
     * Falls back to English if the requested language bundle is not found.
     * Logs an error if a key is missing in the bundle, and returns the key itself as fallback.
     *
     * @param language The target {@link Language}.
     * @param key The key of the message in the resource bundle.
     * @param args Optional arguments to format the message.
     * @return The translated string, or the key if translation is missing.
     */
    public String getTranslation(Language language, String key, Object... args) {
        ResourceBundle bundle = messagesBundles.getOrDefault(language, messagesBundles.get(Language.English));
        try {
            String pattern = bundle.getString(key);
            return MessageFormat.format(pattern, args);
        } catch (java.util.MissingResourceException e) {
            System.err.println("Missing translation for key: " + key + " in language: " + language.name());
            return key;
        }
    }

    /**
     * Retrieves a list of {@link BotCommand}s localized for a specific language.
     * Falls back to English commands if the requested language's commands are not found.
     *
     * @param language The target {@link Language}.
     * @return An unmodifiable {@link List} of localized {@link BotCommand} objects.
     */
    public List<BotCommand> getLocalizedCommands(Language language) {
        return localizedCommands.getOrDefault(language, localizedCommands.get(Language.English));
    }

    /**
     * Retrieves the localized name for a calorie calculation method.
     * Transforms the method name into a consistent resource bundle key format.
     *
     * @param language The target {@link Language}.
     * @param methodName The programmatic name of the method (e.g., "Harris-Benedict").
     * @return The localized name of the method.
     */
    public String getMethodTranslation(Language language, String methodName) {
        String key = "method." + methodName.toLowerCase().replace("-", "_").replace(" ", "_") + ".name";
        return getTranslation(language, key);
    }

    /**
     * Retrieves the localized description for a calorie calculation method.
     * Transforms the method name into a consistent resource bundle key format.
     *
     * @param language The target {@link Language}.
     * @param methodName The programmatic name of the method.
     * @return The localized description of the method.
     */
    public String getMethodDescription(Language language, String methodName) {
        String key = "method." + methodName.toLowerCase().replace("-", "_").replace(" ", "_") + ".description";
        return getTranslation(language, key);
    }

    /**
     * Retrieves the localized string for a {@link Sex} enum value.
     *
     * @param language The target {@link Language}.
     * @param sex The {@link Sex} enum value.
     * @return The localized string for the sex.
     */
    public String getSexTranslation(Language language, Sex sex) {
        return getTranslation(language, "enum.sex." + sex.name().toLowerCase());
    }

    /**
     * Retrieves the localized string for a {@link PhysicalActivityLevel} enum value.
     *
     * @param language The target {@link Language}.
     * @param level The {@link PhysicalActivityLevel} enum value.
     * @return The localized string for the activity level.
     */
    public String getPhysicalActivityLevelTranslation(Language language, PhysicalActivityLevel level) {
        return getTranslation(language, "enum.activity." + level.name().toLowerCase());
    }

    /**
     * Retrieves the localized string for a {@link MainGoal} enum value.
     *
     * @param language The target {@link Language}.
     * @param goal The {@link MainGoal} enum value.
     * @return The localized string for the main goal.
     */
    public String getMainGoalTranslation(Language language, MainGoal goal) {
        return getTranslation(language, "enum.goal." + goal.name().toLowerCase());
    }
}