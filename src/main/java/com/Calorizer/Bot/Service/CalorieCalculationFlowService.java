package com.Calorizer.Bot.Service;

import com.Calorizer.Bot.Model.Enum.Language;
import com.Calorizer.Bot.Model.Enum.MainGoal;
import com.Calorizer.Bot.Model.Enum.PhysicalActivityLevel;
import com.Calorizer.Bot.Model.Enum.Sex;
import com.Calorizer.Bot.Model.User;
import com.Calorizer.Bot.Service.Interface.UserServiceInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for managing the multi-step calorie calculation input flow for users.
 * It guides the user through a series of questions (sex, height, weight, etc.)
 * and stores their input state. Once all data is collected, it triggers the calorie calculation report.
 */
@Service
public class CalorieCalculationFlowService {

    private static final Logger logger = LoggerFactory.getLogger(CalorieCalculationFlowService.class);

    private final UserServiceInt userServiceInt;
    private final LocalizationService localizationService;
    private final MessageSender messageSender;

    private final Map<Long, CalorieInputState> userStates = new HashMap<>();

    /**
     * Inner class to hold the state of a user's calorie input flow.
     * It stores the collected physical data and the current step in the flow.
     */
    private static class CalorieInputState {
        Sex sex;
        double weight;
        double height;
        int age;
        double bodyFatPercent;
        PhysicalActivityLevel activityLevel;
        MainGoal mainGoal;
        String currentStep = "SEX"; // Начальный шаг

        public String getCurrentStep() {
            return currentStep;
        }

        public void setCurrentStep(String currentStep) {
            this.currentStep = currentStep;
        }
    }
    /**
     * Constructor for dependency injection.
     *
     * @param userServiceInt Service for user-related operations, primarily for getting user language.
     * @param localizationService Service for retrieving localized messages.
     * @param messageSender Service for sending messages back to the user.
     */
    public CalorieCalculationFlowService(UserServiceInt userServiceInt, LocalizationService localizationService, MessageSender messageSender) {
        this.userServiceInt = userServiceInt;
        this.localizationService = localizationService;
        this.messageSender = messageSender;
    }

    /**
     * Checks if a specific user is currently engaged in the calorie input flow.
     *
     * @param chatId The Telegram chat ID of the user.
     * @return true if the user has an active calorie input state, false otherwise.
     */
    public boolean isInCalorieInputFlow(Long chatId) {
        return userStates.containsKey(chatId);
    }

    /**
     * Initiates the calorie input flow for a given user.
     * Creates a new {@link CalorieInputState} for the user and sends the first question (sex).
     *
     * @param absSender The {@link AbsSender} instance for sending Telegram responses.
     * @param chatId The Telegram chat ID of the user.
     */
    public void startCalorieInputFlow(AbsSender absSender, Long chatId) {
        userStates.put(chatId, new CalorieInputState());
        askSexStep(absSender, chatId);
        logger.info("Started calorie input flow for user {}.", chatId);
    }

    /**
     * Handles user input during the calorie calculation flow.
     * Based on the current step, it validates the input, updates the state,
     * and sends the next question or the final report.
     *
     * @param absSender The {@link AbsSender} instance for sending Telegram responses.
     * @param chatId The Telegram chat ID of the user.
     * @param text The text message received from the user.
     */
    public void handleCalorieInputStep(AbsSender absSender, Long chatId, String text) {
        CalorieInputState state = userStates.get(chatId);
        if (state == null) {
            logger.warn("Received text for calorie input from user {} but no active state found. Sending unknown command message.", chatId);
            User user = userServiceInt.getOrCreateUser(chatId);
            messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(user.getLanguage(), "error.unknown_command"));
            return;
        }

        User user = userServiceInt.getOrCreateUser(chatId);
        Language lang = user.getLanguage();

        switch (state.getCurrentStep()) {
            case "SEX" -> {
                if ("1".equals(text)) {
                    state.sex = Sex.MALE;
                } else if ("2".equals(text)) {
                    state.sex = Sex.FEMALE;
                } else {
                    messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "error.sex.invalid"));
                    return;
                }
                state.setCurrentStep("HEIGHT");
                messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "question.height"));
            }
            case "HEIGHT" -> {
                try {
                    double height = Double.parseDouble(text);
                    if (height < 50 || height > 300) throw new NumberFormatException();
                    state.height = height;
                    state.setCurrentStep("WEIGHT");
                    messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "question.weight"));
                } catch (NumberFormatException e) {
                    messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "error.height.invalid"));
                }
            }
            case "WEIGHT" -> {
                try {
                    double weight = Double.parseDouble(text);
                    if (weight < 20 || weight > 500) throw new NumberFormatException();
                    state.weight = weight;
                    state.setCurrentStep("AGE");
                    messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "question.age"));
                } catch (NumberFormatException e) {
                    messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "error.weight.invalid"));
                }
            }
            case "AGE" -> {
                try {
                    int age = Integer.parseInt(text);
                    if (age < 5 || age > 120) throw new NumberFormatException();
                    state.age = age;
                    state.setCurrentStep("BODY_FAT");
                    messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "question.body_fat"));
                } catch (NumberFormatException e) {
                    messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "error.age.invalid"));
                }
            }
            case "BODY_FAT" -> {
                try {
                    double bf = Double.parseDouble(text);
                    if (bf < 0 || bf > 70) throw new NumberFormatException();
                    state.bodyFatPercent = bf;
                    state.setCurrentStep("ACTIVITY_LEVEL");
                    sendPhysicalActivityLevelQuestion(absSender, chatId, lang);
                } catch (NumberFormatException e) {
                    messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "error.body_fat.invalid"));
                }
            }
            case "ACTIVITY_LEVEL" -> {
                PhysicalActivityLevel level = parsePhysicalActivityLevel(text);
                if (level == null) {
                    messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "error.activity_level.invalid"));
                    sendPhysicalActivityLevelQuestion(absSender, chatId, lang);
                    return;
                }
                state.activityLevel = level;
                state.setCurrentStep("MAIN_GOAL");
                sendMainGoalQuestion(absSender, chatId, lang);
            }
            case "MAIN_GOAL" -> {
                MainGoal goal = parseMainGoal(text);
                if (goal == null) {
                    messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "error.main_goal.invalid"));
                    sendMainGoalQuestion(absSender, chatId, lang);
                    return;
                }
                state.mainGoal = goal;
                sendCalorieReport(absSender, chatId, state, lang);
                userStates.remove(chatId);
                logger.info("Calorie input flow completed for user {}.", chatId);
            }
        }
    }

    /**
     * Sends the initial question about the user's sex to start the flow.
     *
     * @param absSender The {@link AbsSender} instance for sending Telegram responses.
     * @param chatId The Telegram chat ID of the user.
     */
    private void askSexStep(AbsSender absSender, Long chatId) {
        User user = userServiceInt.getOrCreateUser(chatId);
        Language lang = user.getLanguage();
        String question = localizationService.getTranslation(lang, "question.sex");
        messageSender.sendMessage(absSender, chatId, question);
    }

    /**
     * Sends the question about the user's physical activity level, including options.
     *
     * @param absSender The {@link AbsSender} instance.
     * @param chatId The user's chat ID.
     * @param lang The user's preferred language.
     */
    private void sendPhysicalActivityLevelQuestion(AbsSender absSender, Long chatId, Language lang) {
        String question = localizationService.getTranslation(lang, "question.activity_level");
        messageSender.sendMessage(absSender, chatId, question);
    }

    /**
     * Sends the question about the user's main goal, including options.
     *
     * @param absSender The {@link AbsSender} instance.
     * @param chatId The user's chat ID.
     * @param lang The user's preferred language.
     */
    private void sendMainGoalQuestion(AbsSender absSender, Long chatId, Language lang) {
        String question = localizationService.getTranslation(lang, "question.main_goal");
        messageSender.sendMessage(absSender, chatId, question);
    }

    /**
     * Parses the user's text input to a {@link PhysicalActivityLevel} enum.
     *
     * @param text The user's input string (e.g., "1", "2").
     * @return The corresponding {@link PhysicalActivityLevel} enum, or null if input is invalid.
     */
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

    /**
     * Parses the user's text input to a {@link MainGoal} enum.
     *
     * @param text The user's input string (e.g., "1", "2").
     * @return The corresponding {@link MainGoal} enum, or null if input is invalid.
     */
    private MainGoal parseMainGoal(String text) {
        return switch (text) {
            case "1" -> MainGoal.WEIGHT_LOSS;
            case "2" -> MainGoal.Maintenance;
            case "3" -> MainGoal.WEIGHT_GAIN;
            default -> null;
        };
    }

    /**
     * Calculates the calorie report using {@link FullReportByMethods} and sends it to the user.
     *
     * @param absSender The {@link AbsSender} instance.
     * @param chatId The user's chat ID.
     * @param state The current {@link CalorieInputState} containing all collected data.
     * @param lang The user's preferred language.
     */
    private void sendCalorieReport(AbsSender absSender, Long chatId, CalorieInputState state, Language lang) {
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
        messageSender.sendMessage(absSender, chatId, sb.toString());
    }
}