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

@Service
public class LocalizationService {

    private final Map<Language, List<BotCommand>> localizedCommands = new HashMap<>();
    private final Map<Language, ResourceBundle> messagesBundles = new HashMap<>();

    @PostConstruct
    public void init() {
        for (Language lang : Language.values()) {
            messagesBundles.put(lang, ResourceBundle.getBundle("messages/messages", lang.getLocale()));
        }

        localizedCommands.put(Language.Ukrainian, List.of(
                new BotCommand("/start", getTranslation(Language.Ukrainian, "command.start.description")),
                new BotCommand("/profile", getTranslation(Language.Ukrainian, "command.profile.description")),
                new BotCommand("/changelanguage", getTranslation(Language.Ukrainian, "command.changelanguage.description")),
                new BotCommand("/calculatecalorieforday", getTranslation(Language.Ukrainian, "command.calculatecalorieforday.description"))
        ));
        localizedCommands.put(Language.English, List.of(
                new BotCommand("/start", getTranslation(Language.English, "command.start.description")),
                new BotCommand("/profile", getTranslation(Language.English, "command.profile.description")),
                new BotCommand("/changelanguage", getTranslation(Language.English, "command.changelanguage.description")),
                new BotCommand("/calculatecalorieforday", getTranslation(Language.English, "command.calculatecalorieforday.description"))
        ));
        localizedCommands.put(Language.Russian, List.of(
                new BotCommand("/start", getTranslation(Language.Russian, "command.start.description")),
                new BotCommand("/profile", getTranslation(Language.Russian, "command.profile.description")),
                new BotCommand("/changelanguage", getTranslation(Language.Russian, "command.changelanguage.description")),
                new BotCommand("/calculatecalorieforday", getTranslation(Language.Russian, "command.calculatecalorieforday.description"))
        ));
        localizedCommands.put(Language.German, List.of(
                new BotCommand("/start", getTranslation(Language.German, "command.start.description")),
                new BotCommand("/profile", getTranslation(Language.German, "command.profile.description")),
                new BotCommand("/changelanguage", getTranslation(Language.German, "command.changelanguage.description")),
                new BotCommand("/calculatecalorieforday", getTranslation(Language.German, "command.calculatecalorieforday.description"))
        ));

    }

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

    public List<BotCommand> getLocalizedCommands(Language language) {
        return localizedCommands.getOrDefault(language, localizedCommands.get(Language.English));
    }

    public String getMethodTranslation(Language language, String methodName) {
        String key = "method." + methodName.toLowerCase().replace("-", "_").replace(" ", "_") + ".name";
        return getTranslation(language, key);
    }

    public String getMethodDescription(Language language, String methodName) {
        String key = "method." + methodName.toLowerCase().replace("-", "_").replace(" ", "_") + ".description";
        return getTranslation(language, key);
    }

    public String getSexTranslation(Language language, Sex sex) {
        return getTranslation(language, "enum.sex." + sex.name().toLowerCase());
    }

    public String getPhysicalActivityLevelTranslation(Language language, PhysicalActivityLevel level) {
        return getTranslation(language, "enum.activity." + level.name().toLowerCase());
    }

    public String getMainGoalTranslation(Language language, MainGoal goal) {
        return getTranslation(language, "enum.goal." + goal.name().toLowerCase());
    }
}