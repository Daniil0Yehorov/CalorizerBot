
package com.Calorizer.Bot.MainBot;


import com.Calorizer.Bot.BotConfiguration.BotConfiguration;
import com.Calorizer.Bot.MainBot.CalculateMethods.FullReportByMethods;
import com.Calorizer.Bot.Model.Enum.Language;
import com.Calorizer.Bot.Model.Enum.MainGoal;
import com.Calorizer.Bot.Model.Enum.PhysicalActivityLevel;
import com.Calorizer.Bot.Model.Enum.Sex;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TelegramBot extends TelegramLongPollingBot{

    private final BotConfiguration botConfiguration;
    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    private static final Map<Language, List<BotCommand>> localizedCommands = new HashMap<>();
    private static final Map<Language, Map<String, String>> methodTranslations= new HashMap<>();
    private static final Map<Language, Map<String, String>> methodDescriptions = new HashMap<>();
    static {
        localizedCommands.put(Language.Ukrainian, List.of(
                new BotCommand("/start", "Початок діалогу з ботом"),
                //new BotCommand("/about", "Про бота"),
                new BotCommand("/changelanguage", "Змінити мову"),
                new BotCommand("/calculatecalorieforday", "Калькулятор калорій")
        ));
        localizedCommands.put(Language.English, List.of(
                new BotCommand("/start", "Start interaction with bot"),
                //new BotCommand("/about", "About the bot"),
                new BotCommand("/changelanguage", "Change language"),
                new BotCommand("/calculatecalorieforday", "Calorie calculator")
        ));
        localizedCommands.put(Language.Russian, List.of(
                new BotCommand("/start", "Начало общения с ботом"),
                //new BotCommand("/about", "О боте"),
                new BotCommand("/changelanguage", "Сменить язык"),
                new BotCommand("/calculatecalorieforday", "Калькулятор калорий")
        ));
        localizedCommands.put(Language.German, List.of(
                new BotCommand("/start", "Mit dem Bot kommunizieren"),
                //new BotCommand("/about", "Information über den Bot"),
                new BotCommand("/changelanguage", "Sprache ändern"),
                new BotCommand("/calculatecalorieforday", "Kalorien-Rechner")
        ));
        methodTranslations.put(Language.English, Map.of(
                "Harris-Benedict", "Harris-Benedict",
                "Mifflin-St Jeor", "Mifflin-St Jeor",
                "Katch-McArdle", "Katch-McArdle",
                "Tom Venuto", "Tom Venuto"
        ));
        methodTranslations.put(Language.Ukrainian, Map.of(
                "Harris-Benedict", "Метод Гарріса-Бенедикта",
                "Mifflin-St Jeor", "Метод Міффліна-Сен Жеора",
                "Katch-McArdle", "Метод Кетча-МакАрдла",
                "Tom Venuto", "Метод Тома Венуто"
        ));
        methodTranslations.put(Language.Russian, Map.of(
                "Harris-Benedict", "Метод Гарриса-Бенедикта",
                "Mifflin-St Jeor", "Метод Миффлина-Сен Жеора",
                "Katch-McArdle", "Метод Кетча-МакАрдла",
                "Tom Venuto", "Метод Тома Венуто"
        ));
        methodTranslations.put(Language.English, Map.of(
                "Harris-Benedict", "Harris-Benedict Methode",
                "Mifflin-St Jeor", "Mifflin-St Jeor Methode",
                "Katch-McArdle", "Katch-McArdle Methode",
                "Tom Venuto", "Tom Venuto Methode"
        ));
        methodDescriptions.put(Language.English, Map.of(
                "Harris-Benedict", "Estimates basal metabolic rate (BMR) based on weight, height, age, and sex.",
                "Mifflin-St Jeor", "More accurate BMR estimation using weight, height, age, and sex.",
                "Katch-McArdle", "Calculates BMR based on lean body mass and body fat percentage.",
                "Tom Venuto", "Adjusts BMR with activity level and fitness goals."
        ));
        methodDescriptions.put(Language.Ukrainian, Map.of(
                "Harris-Benedict", "Оцінка базального метаболізму на основі ваги, зросту, віку та статі.",
                "Mifflin-St Jeor", "Точніша оцінка базального метаболізму з урахуванням ваги, зросту, віку та статі.",
                "Katch-McArdle", "Розрахунок базального метаболізму на основі безжирової маси тіла і відсотка жиру.",
                "Tom Venuto", "Коригування базального метаболізму з урахуванням активності та цілей."
        ));
        methodDescriptions.put(Language.Russian, Map.of(
                "Harris-Benedict", "Оценка базального метаболизма на основе веса, роста, возраста и пола.",
                "Mifflin-St Jeor", "Более точная оценка базального метаболизма с учётом веса, роста, возраста и пола.",
                "Katch-McArdle", "Расчёт базального метаболизма на основе безжировой массы тела и процента жира.",
                "Tom Venuto", "Корректировка базального метаболизма с учётом активности и целей."
        ));
        methodDescriptions.put(Language.German, Map.of(
                "Harris-Benedict", "Schätzt den Grundumsatz (BMR) basierend auf Gewicht, Größe, Alter und Geschlecht.",
                "Mifflin-St Jeor", "Genauere BMR-Schätzung unter Verwendung von Gewicht, Größe, Alter und Geschlecht.",
                "Katch-McArdle", "Berechnet BMR basierend auf fettfreier Körpermasse und Körperfettanteil.",
                "Tom Venuto", "Passt den BMR an Aktivitätslevel und Fitnessziele an."
        ));
    }

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
    public TelegramBot(BotConfiguration botConfiguration, UserRepository userRepository) {
        super(botConfiguration.getBotToken());
        this.botConfiguration = botConfiguration;
        this.userRepository = userRepository;
    }

    public void updateCommands(long chatId, Language language) {
        List<BotCommand> commands = localizedCommands.getOrDefault(language, localizedCommands.get(Language.English));
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
        }
        else  if (data.startsWith("AGREE_CALCULATE")){
            userStates.put(chatId, new CalorieInputState());
            askSexStep(chatId);
        }
        else {
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

    private void handleCalorieAgreementStep(Long chatId) {
        User user = userRepository.findByChatId(chatId).orElse(null);
        Language language = user != null ? user.getLanguage() : Language.English;

        String termsText = switch (language) {
            case Ukrainian -> "📄 Умови використання:\n\n" +
                    "🔹 Цей Чат-бот не є повноцінною діагностикою. Результати мають інформаційний характер.\n" +
                    "🔹 Ваші дані в безпеці. Інформація анонімна і не передається третім особам.";
            case Russian -> "📄 Условия использования:\n\n" +
                    "🔹 Этот Чат-бот не является полноценной диагностикой. Результаты предоставлены для информационных целей.\n" +
                    "🔹 Ваши данные в безопасности. Информация анонимна и не передается третьим лицам.";
            case German -> "📄 Nutzungsbedingungen:\n\n" +
                    "🔹 Dieser Chatbot stellt keine medizinische Diagnose dar. Ergebnisse dienen nur zu Informationszwecken.\n" +
                    "🔹 Ihre Daten sind sicher. Die Angaben sind anonym und werden nicht weitergegeben.";
            default -> "📄 Terms of Use:\n\n" +
                    "🔹 This chatbot does not provide medical diagnostics. Results are for informational purposes only.\n" +
                    "🔹 Your data is safe. All information is anonymous and will not be shared.";
        };

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(termsText);

        InlineKeyboardButton agreeButton = new InlineKeyboardButton();
        agreeButton.setText(switch (language) {
            case Ukrainian -> "Я прочитав(ла) і приймаю ✅";
            case Russian -> "Я прочитал(а) и принимаю ✅";
            case German -> "Ich habe gelesen und akzeptiere ✅";
            default -> "I have read and accept ✅";
        });
        agreeButton.setCallbackData("AGREE_CALCULATE");

        List<List<InlineKeyboardButton>> buttons = List.of(List.of(agreeButton));
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(buttons);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending agreement step: {}", e.getMessage());
        }
    }

    private void askSexStep(Long chatId) {
        User user = userRepository.findByChatId(chatId).orElse(null);
        Language lang = user != null ? user.getLanguage() : Language.English;

        String question = switch (lang) {
            case Ukrainian -> "Будь ласка, оберіть вашу стать:\n1 - Чоловік\n2 - Жінка";
            case Russian -> "Пожалуйста, выберите ваш пол:\n1 - Мужчина\n2 - Женщина";
            case German -> "Bitte wählen Sie Ihr Geschlecht:\n1 - Männlich\n2 - Weiblich";
            default -> "Please select your sex:\n1 - Male\n2 - Female";
        };
        sendMessage(chatId, question);
        userStates.get(chatId).currentStep = "SEX";
    }

    private void handleCalorieInputSteps(Long chatId, String text) {
        CalorieInputState state = userStates.get(chatId);
        User user = userRepository.findByChatId(chatId).orElse(null);
        Language lang = user != null ? user.getLanguage() : Language.English;

        switch (state.currentStep) {
            case "SEX" -> {
                if ("1".equals(text)) {
                    state.sex = Sex.MALE;
                } else if ("2".equals(text)) {
                    state.sex = Sex.FEMALE;
                } else {
                    sendMessage(chatId, switch (lang) {
                        case Ukrainian -> "Будь ласка, введіть 1 або 2.";
                        case Russian -> "Пожалуйста, введите 1 или 2.";
                        case German -> "Bitte geben Sie 1 oder 2 ein.";
                        default -> "Please enter 1 or 2.";
                    });
                    return;
                }
                state.currentStep = "HEIGHT";
                sendMessage(chatId, switch (lang) {
                    case Ukrainian -> "Введіть ваш зріст у сантиметрах (наприклад, 175):";
                    case Russian -> "Введите ваш рост в сантиметрах (например, 175):";
                    case German -> "Geben Sie Ihre Größe in Zentimetern ein (z.B. 175):";
                    default -> "Enter your height in centimeters (e.g., 175):";
                });
            }
            case "HEIGHT" -> {
                try {
                    double height = Double.parseDouble(text);
                    if (height < 50 || height > 300) throw new NumberFormatException();
                    state.height = height;
                    state.currentStep = "WEIGHT";
                    sendMessage(chatId, switch (lang) {
                        case Ukrainian -> "Введіть вашу вагу у кілограмах (наприклад, 70):";
                        case Russian -> "Введите ваш вес в килограммах (например, 70):";
                        case German -> "Geben Sie Ihr Gewicht in Kilogramm ein (z.B. 70):";
                        default -> "Enter your weight in kilograms (e.g., 70):";
                    });
                } catch (NumberFormatException e) {
                    sendMessage(chatId, switch (lang) {
                        case Ukrainian -> "Невірний формат. Введіть число для зросту.";
                        case Russian -> "Неверный формат. Введите число для роста.";
                        case German -> "Ungültiges Format. Bitte geben Sie eine Zahl für die Größe ein.";
                        default -> "Invalid format. Please enter a number for height.";
                    });
                }
            }
            case "WEIGHT" -> {
                try {
                    double weight = Double.parseDouble(text);
                    if (weight < 20 || weight > 500) throw new NumberFormatException();
                    state.weight = weight;
                    state.currentStep = "AGE";
                    sendMessage(chatId, switch (lang) {
                        case Ukrainian -> "Введіть ваш вік (наприклад, 30):";
                        case Russian -> "Введите ваш возраст (например, 30):";
                        case German -> "Geben Sie Ihr Alter ein (z.B. 30):";
                        default -> "Enter your age (e.g., 30):";
                    });
                } catch (NumberFormatException e) {
                    sendMessage(chatId, switch (lang) {
                        case Ukrainian -> "Невірний формат. Введіть число для ваги.";
                        case Russian -> "Неверный формат. Введите число для веса.";
                        case German -> "Ungültiges Format. Bitte geben Sie eine Zahl für das Gewicht ein.";
                        default -> "Invalid format. Please enter a number for weight.";
                    });
                }
            }
            case "AGE" -> {
                try {
                    int age = Integer.parseInt(text);
                    if (age < 5 || age > 120) throw new NumberFormatException();
                    state.age = age;
                    state.currentStep = "BODY_FAT";
                    sendMessage(chatId, switch (lang) {
                        case Ukrainian -> "Введіть відсоток жирової тканини (якщо не знаєте, введіть 0):";
                        case Russian -> "Введите процент жировой ткани (если не знаете, введите 0):";
                        case German -> "Geben Sie den Körperfettanteil ein (wenn unbekannt, 0 eingeben):";
                        default -> "Enter your body fat percentage (if unknown, enter 0):";
                    });
                } catch (NumberFormatException e) {
                    sendMessage(chatId, switch (lang) {
                        case Ukrainian -> "Невірний формат. Введіть число для віку.";
                        case Russian -> "Неверный формат. Введите число для возраста.";
                        case German -> "Ungültiges Format. Bitte geben Sie eine Zahl für das Alter ein.";
                        default -> "Invalid format. Please enter a number for age.";
                    });
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
                    sendMessage(chatId, switch (lang) {
                        case Ukrainian -> "Невірний формат. Введіть число для відсотка жиру.";
                        case Russian -> "Неверный формат. Введите число для процента жира.";
                        case German -> "Ungültiges Format. Bitte geben Sie eine Zahl für den Fettanteil ein.";
                        default -> "Invalid format. Please enter a number for body fat percentage.";
                    });
                }
            }
            case "ACTIVITY_LEVEL" -> {
                PhysicalActivityLevel level = parsePhysicalActivityLevel(text);
                if (level == null) {
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
                    sendMainGoalQuestion(chatId, lang);
                    return;
                }
                state.mainGoal = goal;
                sendCalorieReport(chatId, state, lang);
                userStates.remove(chatId);
            }
        }
    }

    private void sendPhysicalActivityLevelQuestion(Long chatId, Language lang) {
        String question = switch (lang) {
            case Ukrainian -> "Оберіть рівень фізичної активності:\n1 - Малорухливий спосіб життя\n2 - Легкі вправи 1-3 рази на тиждень\n3 - Помірні вправи 3-5 разів на тиждень\n4 - Інтенсивні тренування 6-7 разів на тиждень\n5 - Дуже інтенсивні тренування або фізична робота";
            case Russian -> "Выберите уровень физической активности:\n1 - Малоподвижный образ жизни\n2 - Легкие упражнения 1-3 раза в неделю\n3 - Умеренные упражнения 3-5 раз в неделю\n4 - Интенсивные тренировки 6-7 раз в неделю\n5 - Очень интенсивные тренировки или физическая работа";
            case German -> "Wählen Sie das Aktivitätsniveau:\n1 - Wenig aktiv\n2 - Leichte Übungen 1-3 mal pro Woche\n3 - Moderate Übungen 3-5 mal pro Woche\n4 - Intensive Trainingseinheiten 6-7 mal pro Woche\n5 - Sehr intensive Trainingseinheiten oder körperliche Arbeit";
            default -> "Choose your physical activity level:\n1 - Sedentary\n2 - Light exercise 1-3 days/week\n3 - Moderate exercise 3-5 days/week\n4 - Heavy exercise 6-7 days/week\n5 - Very heavy exercise or physical job";
        };
        sendMessage(chatId, question);
    }

    private void sendMainGoalQuestion(Long chatId, Language lang) {
        String question = switch (lang) {
            case Ukrainian -> "Оберіть вашу мету:\n1 - Зниження ваги\n2 - Підтримка ваги\n3 - Набір ваги";
            case Russian -> "Выберите вашу цель:\n1 - Похудение\n2 - Поддержание веса\n3 - Набор веса";
            case German -> "Wählen Sie Ihr Ziel:\n1 - Gewichtsverlust\n2 - Gewicht halten\n3 - Gewicht zunehmen";
            default -> "Choose your main goal:\n1 - Lose weight\n2 - Maintain weight\n3 - Gain weight";
        };
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
        sb.append(switch (lang) {
            case Ukrainian -> "Ваш звіт по калоріях:\n";
            case Russian -> "Ваш отчет по калориям:\n";
            case German -> "Ihr Kalorienbericht:\n";
            default -> "Your calorie report:\n";
        });

        Map<String, String> translations = methodTranslations.getOrDefault(lang, methodTranslations.get(Language.English));
        Map<String, String> descriptions = methodDescriptions.getOrDefault(lang, methodDescriptions.get(Language.English));

        for (Map.Entry<String, Double> entry : report.getResults().entrySet()) {
            String methodName = translations.getOrDefault(entry.getKey(), entry.getKey());
            String methodDesc = descriptions.getOrDefault(entry.getKey(), "");
            sb.append(String.format("%s: %.2f kcal\n", methodName, entry.getValue()));
            if (!methodDesc.isEmpty()) {
                sb.append("  - ").append(methodDesc).append("\n");
            }
            sb.append("\n");
        }

        sendMessage(chatId, sb.toString());
    }

}