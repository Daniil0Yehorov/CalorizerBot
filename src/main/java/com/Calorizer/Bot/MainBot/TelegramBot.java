
package com.Calorizer.Bot.MainBot;


import com.Calorizer.Bot.BotConfiguration.BotConfiguration;
import com.Calorizer.Bot.Model.Enum.Language;
import com.Calorizer.Bot.Model.User;
import com.Calorizer.Bot.Repository.UserRepository;
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
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TelegramBot extends TelegramLongPollingBot{
    @Autowired
    private final BotConfiguration botConfiguration;
    @Autowired
    private UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    private static final Map<Language, List<BotCommand>> localizedCommands = new HashMap<>();
    static {
        localizedCommands.put(Language.Ukrainian, List.of(
                new BotCommand("/start", "Початок діалогу з ботом"),
                //new BotCommand("/about", "Про бота"),
                new BotCommand("/changelanguage", "Змінити мову")
                //new BotCommand("/calculateCalorieForDay", "Калькулятор калорій")
        ));
        localizedCommands.put(Language.English, List.of(
                new BotCommand("/start", "Start interaction with bot"),
                //new BotCommand("/about", "About the bot"),
                new BotCommand("/changelanguage", "Change language")
                //new BotCommand("/calculateCalorieForDay", "Calorie calculator")
        ));
        localizedCommands.put(Language.Russian, List.of(
                new BotCommand("/start", "Начало общения с ботом"),
                //new BotCommand("/about", "О боте"),
                new BotCommand("/changelanguage", "Сменить язык")
                //new BotCommand("/calculateCalorieForDay", "Калькулятор калорий")
        ));
        localizedCommands.put(Language.German, List.of(
                new BotCommand("/start", "Mit dem Bot kommunizieren"),
                //new BotCommand("/about", "Information über den Bot"),
                new BotCommand("/changelanguage", "Sprache ändern")
                //new BotCommand("/calculateCalorieForDay", "Kalorien-Rechner")
        ));
    }

    @Autowired
    public TelegramBot(BotConfiguration botConfiguration) {
        super(botConfiguration.getBotToken());
        this.botConfiguration = botConfiguration;
    }

    public void updateCommands(long chatId, Language language) {
        List<BotCommand> commands = localizedCommands.getOrDefault(language, localizedCommands.get(Language.English));
        try {
            //this.execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
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

            switch (command) {
                case "/start":
                    startCommand(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/changelanguage":
                    sendLanguageSelectionKeyboard(chatId);
                    break;
                default:
                    sendAvailableCommands(chatId);
            }
        }
        else if (update.hasCallbackQuery()) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String data = update.getCallbackQuery().getData();
            handleCallbackQuery(chatId, data);
        }
    }

    private void startCommand(Long chatId, String username) {
        User user = userRepository.findByChatId(chatId).orElseGet(()->{
            User newUser = new User();
            newUser.setChatId(chatId);
            newUser.setLanguage(Language.English);
            return userRepository.save(newUser);
        });

        updateCommands(chatId, user.getLanguage());

        String text = switch (user.getLanguage()) {
            case Ukrainian -> "Привіт, " + username + "! 👋";
            case Russian -> "Привет, " + username + "! 👋";
            case German -> "Hallo, " + username + "! 👋";
            default -> "Hello, " + username + "! 👋";
        };

        sendMessage(chatId, text);
        logger.info("Replied to user " + username);
    }
    private void sendLanguageSelectionKeyboard(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Choose your language / Оберіть мову / Выберите язык / Wählen Sie eine Sprache:");

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
        if (data.startsWith("SET_LANGUAGE_")) {
            String langCode = data.replace("SET_LANGUAGE_", "");
            try {
                Language selectedLang = Language.valueOf(langCode);
                updateUserLanguage(chatId, selectedLang);
            } catch (IllegalArgumentException e) {
                sendMessage(chatId, "Unsupported language.");
            }
        } else {
            sendMessage(chatId, "Unknown command");
        }
    }
    private void updateUserLanguage(Long chatId, Language newLanguage) {
        User user = userRepository.findByChatId(chatId).orElseGet(() -> {
            User newUser = new User();
            newUser.setChatId(chatId);
            return newUser;
        });

        user.setLanguage(newLanguage);
        userRepository.save(user);

        updateCommands(chatId, newLanguage);

        String confirmation = switch (newLanguage) {
            case Ukrainian -> "✅ Мову змінено на українську.";
            case Russian -> "✅ Язык изменен на русский.";
            case German -> "✅ Sprache wurde auf Deutsch geändert.";
            default -> "✅ Language changed to English.";
        };

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
        User user = userRepository.findByChatId(chatId).orElse(null);
        Language lang = user != null ? user.getLanguage() : Language.English;
        List<BotCommand> commands = localizedCommands.getOrDefault(lang, localizedCommands.get(Language.English));

        StringBuilder builder = new StringBuilder();
        builder.append(switch (lang) {
            case Ukrainian -> "📋 Доступні команди:\n\n";
            case Russian -> "📋 Доступные команды:\n\n";
            case German -> "📋 Verfügbare Befehle:\n\n";
            default -> "📋 Available commands:\n\n";
        });

        for (BotCommand command : commands) {
            builder.append(command.getCommand())
                    .append(" - ")
                    .append(command.getDescription())
                    .append("\n");
        }

        sendMessage(chatId, builder.toString());
    }

    /*public static InlineKeyboardMarkup createKeyboard() {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> firstRow = new ArrayList<>();
        firstRow.add(createButton("Add Me", "/addMe"));
        firstRow.add(createButton("Me", "/me"));
        firstRow.add(createButton("Remove Me", "/removeMe"));

        List<InlineKeyboardButton> secondRow = new ArrayList<>();
        secondRow.add(createButton("List Users", "/listUsers"));

        rows.add(firstRow);
        rows.add(secondRow);

        keyboardMarkup.setKeyboard(rows);
        return keyboardMarkup;
    }

    private static InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }
      private void sendMessageWithKeyboard(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(createKeyboard());
        try {execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error occured"+e.getMessage());
        }
    }*/
}