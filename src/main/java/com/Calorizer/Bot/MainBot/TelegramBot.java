package com.Calorizer.Bot.MainBot;

import com.Calorizer.Bot.BotConfiguration.BotConfiguration;
import com.Calorizer.Bot.MainBot.CalculateMethods.FullReportByMethods;
import com.Calorizer.Bot.Model.Enum.Language;
import com.Calorizer.Bot.Model.Enum.MainGoal;
import com.Calorizer.Bot.Model.Enum.PhysicalActivityLevel;
import com.Calorizer.Bot.Model.Enum.Sex;
import com.Calorizer.Bot.Model.User;
import com.Calorizer.Bot.Service.Interface.UserServiceInt;
import com.Calorizer.Bot.Service.LocalizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeChat;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TelegramBot extends TelegramLongPollingBot{

    @Autowired
    private  BotConfiguration botConfiguration;
    @Autowired
    private UserServiceInt userServiceInt;

    @Autowired
    private LocalizationService localizationService;

    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    private Map<Long, CalorieInputState> userStates = new HashMap<>();

    private static class CalorieInputState {
        Sex sex;
        double weight;
        double height;
        int age;
        double bodyFatPercent;
        PhysicalActivityLevel activityLevel;
        MainGoal mainGoal;
        String currentStep = "SEX";
    }

    @Autowired
    public TelegramBot(BotConfiguration botConfiguration){ super(botConfiguration.getBotToken());
        this.botConfiguration = botConfiguration;}

    public void updateCommands(long chatId, Language language) {
        List<BotCommand> commands = localizationService.getLocalizedCommands(language);
        try {
            this.execute(new SetMyCommands(commands, new BotCommandScopeChat(String.valueOf(chatId)), null));
        } catch (TelegramApiException e) {
            logger.error("Failed to update commands: {}", e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return botConfiguration.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfiguration.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String command = update.getMessage().getText();
            if (userStates.containsKey(chatId)) {
                handleCalorieInputSteps(chatId, command);
                return;
            }
            switch (command) {
                case "/start" -> startCommand(chatId, update.getMessage().getChat().getFirstName());
                case "/changelanguage" -> sendLanguageSelectionKeyboard(chatId);
                case "/calculatecalorieforday" -> handleCalorieAgreementStep(chatId);
                case "/profile" -> showprofile(chatId);
                default -> sendAvailableCommands(chatId);
            }
        }
        else if (update.hasCallbackQuery()) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String data = update.getCallbackQuery().getData();
            handleCallbackQuery(chatId, data);
        }
    }

    private void startCommand(Long chatId, String username) {
        User user = userServiceInt.getOrCreateUser(chatId);

        updateCommands(chatId, user.getLanguage());

        String greetingTemplate = localizationService.getTranslation(user.getLanguage(), "greeting");
        String text = MessageFormat.format(greetingTemplate, username);

        sendMessage(chatId, text);
        logger.info("Replied to user " + username);
    }

    private void sendLanguageSelectionKeyboard(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(localizationService.getTranslation(Language.English, "language.selection.prompt"));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(createLanguageButton("🇬🇧 English", Language.English)));
        rows.add(List.of(createLanguageButton("🇺🇦 Українська", Language.Ukrainian)));
        rows.add(List.of(createLanguageButton("🇷🇺 Русский", Language.Russian)));
        rows.add(List.of(createLanguageButton("🇩🇪 Deutsch", Language.German)));

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending language selection keyboard: {}", e.getMessage());
        }
    }

    private InlineKeyboardButton createLanguageButton(String text, Language language) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData("SET_LANGUAGE_" + language.name());
        return button;
    }

    private void handleCallbackQuery(Long chatId, String data) {
        User user = userServiceInt.getOrCreateUser(chatId);
        Language language = user.getLanguage();

        if (data.startsWith("SET_LANGUAGE_")) {
            String langCode = data.replace("SET_LANGUAGE_", "");
            try {
                Language selectedLang = Language.valueOf(langCode);
                updateUserLanguage(chatId, selectedLang);
            } catch (IllegalArgumentException e) {
                sendMessage(chatId, localizationService.getTranslation(language, "language.unsupported"));
            }
        }

        else  if (data.startsWith("AGREE_CALCULATE")){
            userStates.put(chatId, new CalorieInputState());
            askSexStep(chatId);
        }

        else if (data.startsWith("DISAGREE_CALCULATE")){
            sendMessage(chatId, localizationService.getTranslation(language, "terms.of.use.disagree_message"));
        }

        else {sendMessage(chatId, localizationService.getTranslation(language, "error.unknown_command"));}
    }

    private void updateUserLanguage(Long chatId, Language newLanguage) {
        User user = userServiceInt.getOrCreateUser(chatId);
        Language oldLanguage = user.getLanguage();

        user.setLanguage(newLanguage);
        userServiceInt.save(user);

        updateCommands(chatId, newLanguage);

        String confirmation;
        switch (newLanguage) {
            case Ukrainian:
                confirmation = localizationService.getTranslation(oldLanguage, "language.set.ukrainian");
                break;
            case Russian:
                confirmation = localizationService.getTranslation(oldLanguage, "language.set.russian");
                break;
            case German:
                confirmation = localizationService.getTranslation(oldLanguage, "language.set.german");
                break;
            default:
                confirmation = localizationService.getTranslation(oldLanguage, "language.set.english");
                break;
        }

        sendMessage(chatId, confirmation);
    }

    private void sendMessage(Long chatid,String text){
        SendMessage message=new SendMessage();
        message.setChatId(chatid);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error occured"+e.getMessage());
        }
    }

    private void sendAvailableCommands(Long chatId) {
        User user = userServiceInt.getOrCreateUser(chatId);
        Language lang = user != null ? user.getLanguage() : Language.English;
        List<BotCommand> commands=localizationService.getLocalizedCommands(lang);
        StringBuilder builder = new StringBuilder();

        builder.append(localizationService.getTranslation(lang, "command.available_commands")).append("\n\n");

        for (BotCommand command : commands) {
            builder.append(command.getCommand())
                    .append(" - ")
                    .append(command.getDescription())
                    .append("\n");
        }

        sendMessage(chatId, builder.toString());
    }

    private void handleCalorieAgreementStep(Long chatId) {
        User user = userServiceInt.getOrCreateUser(chatId);
        Language language = user.getLanguage();

        String termsTitle = localizationService.getTranslation(language, "terms.of.use.title");
        String disclaimer = localizationService.getTranslation(language, "terms.of.use.disclaimer");
        String dataSafety = localizationService.getTranslation(language, "terms.of.use.data_safety");

        String termsText = termsTitle + "\n\n" + disclaimer + "\n" + dataSafety;

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(termsText);

        InlineKeyboardButton agreeButton = new InlineKeyboardButton();
        agreeButton.setText(localizationService.getTranslation(language, "terms.of.use.agree_button"));
        agreeButton.setCallbackData("AGREE_CALCULATE");

        InlineKeyboardButton disagreeButton = new InlineKeyboardButton();
        disagreeButton.setText(localizationService.getTranslation(language, "terms.of.use.disagree_button"));
        disagreeButton.setCallbackData("DISAGREE_CALCULATE");

        List<List<InlineKeyboardButton>> buttons = List.of(List.of(disagreeButton,agreeButton));
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(buttons);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending agreement step: {}", e.getMessage());
        }
    }

    private void askSexStep(Long chatId) {
        User user = userServiceInt.getOrCreateUser(chatId);
        Language lang = user.getLanguage();

        String question = localizationService.getTranslation(lang, "question.sex");
        sendMessage(chatId, question);
        userStates.get(chatId).currentStep = "SEX";
    }

    private void handleCalorieInputSteps(Long chatId, String text) {
        CalorieInputState state = userStates.get(chatId);
        User user = userServiceInt.getOrCreateUser(chatId);
        Language lang = user.getLanguage();

        switch (state.currentStep) {
            case "SEX" -> {
                if ("1".equals(text)) {
                    state.sex = Sex.MALE;
                } else if ("2".equals(text)) {
                    state.sex = Sex.FEMALE;
                } else {
                    sendMessage(chatId, localizationService.getTranslation(lang, "error.sex.invalid"));
                    return;
                }

                state.currentStep = "HEIGHT";
                sendMessage(chatId, localizationService.getTranslation(lang, "question.height"));
            }
            case "HEIGHT" -> {
                try {
                    double height = Double.parseDouble(text);
                    if (height < 50 || height > 300) throw new NumberFormatException();
                    state.height = height;
                    state.currentStep = "WEIGHT";
                    sendMessage(chatId, localizationService.getTranslation(lang, "question.weight"));
                } catch (NumberFormatException e) {
                    sendMessage(chatId, localizationService.getTranslation(lang, "error.height.invalid"));
                }
            }
            case "WEIGHT" -> {
                try {
                    double weight = Double.parseDouble(text);
                    if (weight < 20 || weight > 500) throw new NumberFormatException();
                    state.weight = weight;
                    state.currentStep = "AGE";
                    sendMessage(chatId, localizationService.getTranslation(lang, "question.age"));
                } catch (NumberFormatException e) {
                    sendMessage(chatId, localizationService.getTranslation(lang, "error.weight.invalid"));
                }
            }
            case "AGE" -> {
                try {
                    int age = Integer.parseInt(text);
                    if (age < 5 || age > 120) throw new NumberFormatException();
                    state.age = age;
                    state.currentStep = "BODY_FAT";
                    sendMessage(chatId, localizationService.getTranslation(lang, "question.body_fat"));
                } catch (NumberFormatException e) {
                    sendMessage(chatId, localizationService.getTranslation(lang, "error.age.invalid"));
                }
            }
            case "BODY_FAT" -> {
                try {
                    double bf = Double.parseDouble(text);
                    if (bf < 0 || bf > 70) throw new NumberFormatException();
                    state.bodyFatPercent = bf;
                    state.currentStep = "ACTIVITY_LEVEL";
                    sendPhysicalActivityLevelQuestion(chatId, lang);
                } catch (NumberFormatException e) {
                    sendMessage(chatId, localizationService.getTranslation(lang, "error.body_fat.invalid"));
                }
            }
            case "ACTIVITY_LEVEL" -> {
                PhysicalActivityLevel level = parsePhysicalActivityLevel(text);
                if (level == null) {
                    sendMessage(chatId, localizationService.getTranslation(lang, "error.activity_level.invalid"));
                    sendPhysicalActivityLevelQuestion(chatId, lang);
                    return;
                }
                state.activityLevel = level;
                state.currentStep = "MAIN_GOAL";
                sendMainGoalQuestion(chatId, lang);
            }
            case "MAIN_GOAL" -> {
                MainGoal goal = parseMainGoal(text);
                if (goal == null) {
                    sendMessage(chatId, localizationService.getTranslation(lang, "error.main_goal.invalid"));
                    sendMainGoalQuestion(chatId, lang);
                    return;
                }
                sendCalorieReport(chatId, state, lang);
                userStates.remove(chatId);
            }
        }
    }

    private void sendPhysicalActivityLevelQuestion(Long chatId, Language lang) {
        String question = localizationService.getTranslation(lang, "question.activity_level");
        sendMessage(chatId, question);
    }

    private void sendMainGoalQuestion(Long chatId, Language lang) {
        String question = localizationService.getTranslation(lang, "question.main_goal");
        sendMessage(chatId, question);
    }

    private PhysicalActivityLevel parsePhysicalActivityLevel(String text) {
        return switch (text) {
            case "1" -> PhysicalActivityLevel.SEDENTARY;
            case "2" -> PhysicalActivityLevel.LIGHT;
            case "3" -> PhysicalActivityLevel.MODERATE;
            case "4" -> PhysicalActivityLevel.ACTIVE;
            case "5" -> PhysicalActivityLevel.VERY_ACTIVE;
            default -> null;
        };
    }

    private MainGoal parseMainGoal(String text) {
        return switch (text) {
            case "1" -> MainGoal.WEIGHT_LOSS;
            case "2" -> MainGoal.Maintenance;
            case "3" -> MainGoal.WEIGHT_GAIN;
            default -> null;
        };
    }

    private void sendCalorieReport(Long chatId, CalorieInputState state, Language lang) {
        FullReportByMethods report = new FullReportByMethods(
                state.sex,
                state.weight,
                state.height,
                state.age,
                state.bodyFatPercent,
                state.activityLevel,
                state.mainGoal
        );

        StringBuilder sb = new StringBuilder();
        sb.append(localizationService.getTranslation(lang, "report.calorie.title")).append("\n");

        for (Map.Entry<String, Double> entry : report.getResults().entrySet()) {
            String methodName = localizationService.getMethodTranslation(lang, entry.getKey());
            String methodDesc = localizationService.getMethodDescription(lang, entry.getKey());
            sb.append(String.format("%s: %.2f kcal\n", methodName, entry.getValue()));
            if (!methodDesc.isEmpty()) {
                sb.append("  - ").append(methodDesc).append("\n");
            }
            sb.append("\n");
        }

        sendMessage(chatId, sb.toString());
    }

    private void showprofile(Long chatId){
        String message = userServiceInt.getProfileMessage(chatId);
        sendMessage(chatId, message);
    }

}