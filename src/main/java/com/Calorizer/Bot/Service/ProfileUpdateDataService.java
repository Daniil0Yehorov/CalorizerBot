package com.Calorizer.Bot.Service;

import com.Calorizer.Bot.Model.Enum.Language;
import com.Calorizer.Bot.Model.Enum.MainGoal;
import com.Calorizer.Bot.Model.Enum.PhysicalActivityLevel;
import com.Calorizer.Bot.Model.Enum.Sex;
import com.Calorizer.Bot.Model.User;
import com.Calorizer.Bot.Model.UserPhysicalData;
import com.Calorizer.Bot.Service.Interface.UserServiceInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.*;

/**
 * Service responsible for managing the multi-step profile data update flow for users.
 * This service guides the user through a series of questions (e.g., sex, height, weight)
 * to collect or update their physical data. It supports two modes:
 * <ul>
 * <li>Full profile update ({@link UpdateMode#ALL_ATTRIBUTES}): Guides the user through all attributes sequentially.</li>
 * <li>Single attribute update ({@link UpdateMode#SINGLE_ATTRIBUTE}): Allows the user to select and modify one specific attribute.</li>
 * </ul>
 * The service maintains the user's temporary input state until all necessary data is collected
 * and then persists it to the database. It also handles input validation and localization.
 */
@Service
public class ProfileUpdateDataService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileUpdateDataService.class);

    private final UserServiceInt userServiceInt;
    private final LocalizationService localizationService;
    private final MessageSender messageSender;

    /**
     * Defines the mode of profile update.
     */
    public enum UpdateMode {
        /** User updates all physical data attributes sequentially. */
        ALL_ATTRIBUTES,
        /** User updates one specific attribute at a time by selecting it from a list. */
        SINGLE_ATTRIBUTE
    }

    /**
     * Represents the transient state of a user during the profile update flow.
     * This state is stored in memory (a HashMap) and is crucial for guiding
     * the user through multi-step inputs in a conversational manner.
     */
    private static class ProfileUpdateState {
        // The mode of update chosen by the user (full or single attribute)
        UpdateMode mode;
        // The current step in the update flow, indicating what input is expected next from the user.
        String currentStep;
        // In SINGLE_ATTRIBUTE mode, this field holds the name of the attribute currently being updated.
        String attributeToUpdate;
        // Temporary storage for the user's physical data during the update process.
        // These fields are populated as the user provides input or from existing user data.

        Sex sex;
        double weight;
        double height;
        int age;
        double bodyFatPercent;
        PhysicalActivityLevel activityLevel;
        MainGoal mainGoal;

        /**
         * Initializes a new state for a profile update flow.
         * Sets the initial step based on the provided update mode.
         *
         * @param mode The mode of the profile update (ALL_ATTRIBUTES or SINGLE_ATTRIBUTE).
         */
        public ProfileUpdateState(UpdateMode mode) {
            this.mode = mode;

            if (mode == UpdateMode.ALL_ATTRIBUTES) {
                this.currentStep = "SEX";
            } else {
                this.currentStep = "SELECT_ATTRIBUTE";
            }
        }

        public String getCurrentStep() {
            return currentStep;
        }

        public void setCurrentStep(String currentStep) {
            this.currentStep = currentStep;
        }

        public UpdateMode getMode() {
            return mode;
        }

        public String getAttributeToUpdate() {
            return attributeToUpdate;
        }

        public void setAttributeToUpdate(String attributeToUpdate) {
            this.attributeToUpdate = attributeToUpdate;
        }
    }

    private final Map<Long, ProfileUpdateState> userStates = new HashMap<>();

    /**
     * Constructs the ProfileUpdateDataService, injecting its dependencies.
     * Spring automatically provides instances of these services.
     *
     * @param userServiceInt Service for managing user data, including retrieval and persistence.
     * @param localizationService Service for fetching localized messages based on user language.
     * @param messageSender Service for sending messages and keyboards back to the Telegram user.
     */
    public ProfileUpdateDataService(UserServiceInt userServiceInt, LocalizationService localizationService, MessageSender messageSender) {
        this.userServiceInt = userServiceInt;
        this.localizationService = localizationService;
        this.messageSender = messageSender;
    }

    /**
     * Checks if a specific user (identified by their chat ID) is currently
     * in an active profile update flow.
     *
     * @param chatId The chat ID of the user.
     * @return {@code true} if the user has an active {@link ProfileUpdateState}, {@code false} otherwise.
     */
    public boolean isInProfileUpdateFlow(Long chatId) {
        return userStates.containsKey(chatId);
    }

    /**
     * Initiates a new profile update flow for a given user.
     * A new {@link ProfileUpdateState} is created and stored, and it's pre-populated
     * with any existing physical data for the user.
     * The initial prompt (either asking for sex or presenting attribute selection)
     * is then sent to the user based on the chosen {@link UpdateMode}.
     *
     * @param absSender The {@link AbsSender} instance used to send messages to Telegram.
     * @param chatId The chat ID of the user starting the flow.
     * @param mode The desired {@link UpdateMode} ({@code ALL_ATTRIBUTES} for sequential update or {@code SINGLE_ATTRIBUTE} for individual attribute update).
     */
    public void startProfileUpdateFlow(AbsSender absSender, Long chatId, UpdateMode mode) {
        ProfileUpdateState state = new ProfileUpdateState(mode);
        userStates.put(chatId, state);

        User user = userServiceInt.getOrCreateUser(chatId);
        UserPhysicalData physicalData = user.getUPD();

        if (physicalData != null) {
            state.sex = physicalData.getSex();
            state.weight = physicalData.getWeight();
            state.height = physicalData.getHeight();
            state.age = physicalData.getAge();
            state.bodyFatPercent = physicalData.getBodyFatPercent();
            state.activityLevel = physicalData.getPhysicalActivityLevel();
            state.mainGoal = physicalData.getMaingoal();
        }

        if (mode == UpdateMode.ALL_ATTRIBUTES) {
            askSexStep(absSender, chatId);
            logger.info("Started full profile update flow for user {}.", chatId);
        } else {
            sendAttributeSelectionKeyboard(absSender, chatId, user.getLanguage());
            logger.info("Started single attribute profile update flow for user {}.", chatId);
        }
    }

    /**
     * Handles an incoming text message from a user who is currently in a profile update flow.
     * This method validates the input based on the current step and the update mode,
     * updates the user's temporary state, and prompts for the next piece of information.
     * If the input is invalid, an error message is sent.
     *
     * @param absSender The {@link AbsSender} instance for sending messages.
     * @param chatId The chat ID of the user providing input.
     * @param text The text message received from the user.
     */
    public void handleProfileInputStep(AbsSender absSender, Long chatId, String text) {
        User user = userServiceInt.getOrCreateUser(chatId);
        Language lang = user.getLanguage();


        if (!user.isPayedAcc()) {
            messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "profile.access_paid_only"));
            userStates.remove(chatId);
            logger.warn("Non-paid user {} tried to input data in profile update flow.", chatId);
            return;
        }

        ProfileUpdateState state = userStates.get(chatId);

        if (state == null) {
            logger.warn("Received text for profile update from user {} but no active state found.", chatId);
            messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "error.unknown_command"));
            return;
        }

        if (state.getMode() == UpdateMode.ALL_ATTRIBUTES) {
            handleAllAttributesFlow(absSender, chatId, text, state, user, lang);
        } else {
            handleSingleAttributeFlow(absSender, chatId, text, state, user, lang);
        }
    }

    /**
     * Handles the sequential processing of input for the "all attributes" profile update flow.
     * It uses a switch statement based on the {@code currentStep} to determine which attribute
     * is currently expected, validates the input, updates the state, and prompts for the next step.
     *
     * @param absSender The {@link AbsSender} instance for sending messages.
     * @param chatId The chat ID of the user.
     * @param text The user's input for the current step.
     * @param state The current {@link ProfileUpdateState} of the user.
     * @param user The {@link User} entity for the current user.
     * @param lang The user's preferred {@link Language}.
     */
    private void handleAllAttributesFlow(AbsSender absSender, Long chatId, String text, ProfileUpdateState state, User user, Language lang) {
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
                saveUpdatedProfileData(absSender, chatId, state, user, lang);
                userStates.remove(chatId);
                logger.info("Full profile update flow completed for user {}.", chatId);
            }
        }
    }

    /**
     * Handles the processing of input for the "single attribute" profile update flow.
     * It determines which specific attribute the user is currently updating (based on {@code currentStep})
     * validates the input for that attribute, updates the temporary state, and then
     * saves the single attribute. After saving, it returns the user to the attribute selection keyboard.
     *
     * @param absSender The {@link AbsSender} instance for sending messages.
     * @param chatId The chat ID of the user.
     * @param text The user's input for the current attribute.
     * @param state The current {@link ProfileUpdateState} of the user.
     * @param user The {@link User} entity for the current user.
     * @param lang The user's preferred {@link Language}.
     */
    private void handleSingleAttributeFlow(AbsSender absSender, Long chatId, String text, ProfileUpdateState state, User user, Language lang) {
        switch (state.getCurrentStep()) {
            case "SELECT_ATTRIBUTE" -> {
                messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "error.select_attribute_first"));
                sendAttributeSelectionKeyboard(absSender, chatId, lang); // Повторно предлагаем выбрать
            }
            case "AWAITING_SEX_INPUT" -> {
                if ("1".equals(text)) {
                    state.sex = Sex.MALE;
                } else if ("2".equals(text)) {
                    state.sex = Sex.FEMALE;
                } else {
                    messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "error.sex.invalid"));
                    return;
                }
                saveSingleAttribute(absSender, chatId, state, user, lang);
            }
            case "AWAITING_HEIGHT_INPUT" -> {
                try {
                    double height = Double.parseDouble(text);
                    if (height < 50 || height > 300) throw new NumberFormatException();
                    state.height = height;
                    saveSingleAttribute(absSender, chatId, state, user, lang);
                } catch (NumberFormatException e) {
                    messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "error.height.invalid"));
                }
            }
            case "AWAITING_WEIGHT_INPUT" -> {
                try {
                    double weight = Double.parseDouble(text);
                    if (weight < 20 || weight > 500) throw new NumberFormatException();
                    state.weight = weight;
                    saveSingleAttribute(absSender, chatId, state, user, lang);
                } catch (NumberFormatException e) {
                    messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "error.weight.invalid"));
                }
            }
            case "AWAITING_AGE_INPUT" -> {
                try {
                    int age = Integer.parseInt(text);
                    if (age < 5 || age > 120) throw new NumberFormatException();
                    state.age = age;
                    saveSingleAttribute(absSender, chatId, state, user, lang);
                } catch (NumberFormatException e) {
                    messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "error.age.invalid"));
                }
            }
            case "AWAITING_BODY_FAT_INPUT" -> {
                try {
                    double bf = Double.parseDouble(text);
                    if (bf < 0 || bf > 70) throw new NumberFormatException();
                    state.bodyFatPercent = bf;
                    saveSingleAttribute(absSender, chatId, state, user, lang);
                } catch (NumberFormatException e) {
                    messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "error.body_fat.invalid"));
                }
            }
            case "AWAITING_ACTIVITY_LEVEL_INPUT" -> {
                PhysicalActivityLevel level = parsePhysicalActivityLevel(text);
                if (level == null) {
                    messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "error.activity_level.invalid"));
                    sendPhysicalActivityLevelQuestion(absSender, chatId, lang); // Повторно отправляем вопрос
                    return;
                }
                state.activityLevel = level;
                saveSingleAttribute(absSender, chatId, state, user, lang);
            }
            case "AWAITING_MAIN_GOAL_INPUT" -> {
                MainGoal goal = parseMainGoal(text);
                if (goal == null) {
                    messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "error.main_goal.invalid"));
                    sendMainGoalQuestion(absSender, chatId, lang); // Повторно отправляем вопрос
                    return;
                }
                state.mainGoal = goal;
                saveSingleAttribute(absSender, chatId, state, user, lang);
            }
            default -> {
                logger.warn("Unexpected state in single attribute flow for user {}: {}", chatId, state.getCurrentStep());
                messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "error.unexpected_state"));
                userStates.remove(chatId);
            }
        }
    }

    /**
     * Sends a message to the user asking for their sex (gender) input.
     *
     * @param absSender The {@link AbsSender} instance for sending messages.
     * @param chatId The chat ID of the user.
     */
    private void askSexStep(AbsSender absSender, Long chatId) {
        User user = userServiceInt.getOrCreateUser(chatId);
        Language lang = user.getLanguage();
        String question = localizationService.getTranslation(lang, "question.sex");
        messageSender.sendMessage(absSender, chatId, question);
    }

    /**
     * Sends a message to the user asking for their physical activity level,
     * typically providing numbered options to choose from.
     *
     * @param absSender The {@link AbsSender} instance for sending messages.
     * @param chatId The chat ID of the user.
     * @param lang The user's preferred {@link Language} for localization.
     */
    private void sendPhysicalActivityLevelQuestion(AbsSender absSender, Long chatId, Language lang) {
        String question = localizationService.getTranslation(lang, "question.activity_level");
        messageSender.sendMessage(absSender, chatId, question);
    }

    /**
     * Sends a message to the user asking for their main goal (e.g., weight loss, maintenance, weight gain),
     * typically providing numbered options to choose from.
     *
     * @param absSender The {@link AbsSender} instance for sending messages.
     * @param chatId The chat ID of the user.
     * @param lang The user's preferred {@link Language} for localization.
     */
    private void sendMainGoalQuestion(AbsSender absSender, Long chatId, Language lang) {
        String question = localizationService.getTranslation(lang, "question.main_goal");
        messageSender.sendMessage(absSender, chatId, question);
    }

    /**
     * Parses the user's text input into a {@link PhysicalActivityLevel} enumeration.
     * It maps numerical strings ("1" through "5") to corresponding activity levels.
     *
     * @param text The user's input string.
     * @return The corresponding {@link PhysicalActivityLevel} enum, or {@code null} if the input is invalid.
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
     * Parses the user's text input into a {@link MainGoal} enumeration.
     * It maps numerical strings ("1" through "3") to corresponding main goals.
     *
     * @param text The user's input string.
     * @return The corresponding {@link MainGoal} enum, or {@code null} if the input is invalid.
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
     * Sends an inline keyboard to the user, allowing them to select which specific
     * profile attribute they wish to update in the "single attribute" mode.
     * Includes a "Done" button to exit this mode.
     *
     * @param absSender The {@link AbsSender} instance for sending messages.
     * @param chatId The chat ID of the user.
     * @param lang The user's preferred {@link Language} for localization.
     */
    private void sendAttributeSelectionKeyboard(AbsSender absSender, Long chatId, Language lang) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(localizationService.getTranslation(lang, "question.profile.update.select_attribute"));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();


        InlineKeyboardButton buttonSex = new InlineKeyboardButton(localizationService.getTranslation(lang, "profile.label.sex"));
        buttonSex.setCallbackData("UPDATE_SEX");
        rows.add(List.of(buttonSex));

        InlineKeyboardButton buttonHeight = new InlineKeyboardButton(localizationService.getTranslation(lang, "profile.label.height"));
        buttonHeight.setCallbackData("UPDATE_HEIGHT");
        rows.add(List.of(buttonHeight));

        InlineKeyboardButton buttonWeight = new InlineKeyboardButton(localizationService.getTranslation(lang, "profile.label.weight"));
        buttonWeight.setCallbackData("UPDATE_WEIGHT");
        rows.add(List.of(buttonWeight));

        InlineKeyboardButton buttonAge = new InlineKeyboardButton(localizationService.getTranslation(lang, "profile.label.age"));
        buttonAge.setCallbackData("UPDATE_AGE");
        rows.add(List.of(buttonAge));

        InlineKeyboardButton buttonBodyFat = new InlineKeyboardButton(localizationService.getTranslation(lang, "profile.label.body_fat"));
        buttonBodyFat.setCallbackData("UPDATE_BODY_FAT");
        rows.add(List.of(buttonBodyFat));

        InlineKeyboardButton buttonActivityLevel = new InlineKeyboardButton(localizationService.getTranslation(lang, "profile.label.activity_level"));
        buttonActivityLevel.setCallbackData("UPDATE_ACTIVITY_LEVEL");
        rows.add(List.of(buttonActivityLevel));

        InlineKeyboardButton buttonMainGoal = new InlineKeyboardButton(localizationService.getTranslation(lang, "profile.label.main_goal"));
        buttonMainGoal.setCallbackData("UPDATE_MAIN_GOAL");
        rows.add(List.of(buttonMainGoal));

        InlineKeyboardButton buttonDone = new InlineKeyboardButton(localizationService.getTranslation(lang, "button.done"));
        buttonDone.setCallbackData("UPDATE_PROFILE_DONE");
        rows.add(List.of(buttonDone));

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        messageSender.sendMessage(absSender, message);
    }

    /**
     * Handles callback queries when a user selects an attribute to update
     * from the inline keyboard presented in "single attribute" mode.
     * It sets the current step to await input for the chosen attribute and prompts the user.
     * Also handles the "Done" callback to end the flow.
     *
     * @param absSender The {@link AbsSender} instance for sending messages.
     * @param chatId The chat ID of the user.
     * @param callbackData The callback data string from the selected inline button.
     */
    public void handleAttributeSelectionCallback(AbsSender absSender, Long chatId, String callbackData) {
        User user = userServiceInt.getOrCreateUser(chatId);
        Language lang = user.getLanguage();

        if (!user.isPayedAcc()) {
            messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "profile.access_paid_only"));
            userStates.remove(chatId);
            logger.warn("Non-paid user {} tried to interact with profile update flow callbacks.", chatId);
            return;
        }

        ProfileUpdateState state = userStates.get(chatId);

        if (state == null) {

            logger.warn("Received unexpected attribute selection callback '{}' from user {} (no active flow).", callbackData, chatId);
            messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "error.unexpected_action"));
            userStates.remove(chatId);
            return;
        }

        if ("UPDATE_PROFILE_DONE".equals(callbackData)) {
            messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "message.profile.update.completed"));
            userStates.remove(chatId);
            logger.info("Single attribute profile update flow completed for user {}.", chatId);
            return;
        }

        switch (callbackData) {
            case "UPDATE_SEX" -> {
                state.setAttributeToUpdate("SEX");
                state.setCurrentStep("AWAITING_SEX_INPUT");
                askSexStep(absSender, chatId);
            }
            case "UPDATE_HEIGHT" -> {
                state.setAttributeToUpdate("HEIGHT");
                state.setCurrentStep("AWAITING_HEIGHT_INPUT");
                messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "question.height"));
            }
            case "UPDATE_WEIGHT" -> {
                state.setAttributeToUpdate("WEIGHT");
                state.setCurrentStep("AWAITING_WEIGHT_INPUT");
                messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "question.weight"));
            }
            case "UPDATE_AGE" -> {
                state.setAttributeToUpdate("AGE");
                state.setCurrentStep("AWAITING_AGE_INPUT");
                messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "question.age"));
            }
            case "UPDATE_BODY_FAT" -> {
                state.setAttributeToUpdate("BODY_FAT");
                state.setCurrentStep("AWAITING_BODY_FAT_INPUT");
                messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "question.body_fat"));
            }
            case "UPDATE_ACTIVITY_LEVEL" -> {
                state.setAttributeToUpdate("ACTIVITY_LEVEL");
                state.setCurrentStep("AWAITING_ACTIVITY_LEVEL_INPUT");
                sendPhysicalActivityLevelQuestion(absSender, chatId, lang);
            }
            case "UPDATE_MAIN_GOAL" -> {
                state.setAttributeToUpdate("MAIN_GOAL");
                state.setCurrentStep("AWAITING_MAIN_GOAL_INPUT");
                sendMainGoalQuestion(absSender, chatId, lang);
            }
            default -> {

                logger.warn("Unknown attribute selection callback for user {}: {}", chatId, callbackData);
                messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "error.unexpected_action"));
                 sendAttributeSelectionKeyboard(absSender, chatId, lang);
            }
        }
    }

    /**
     * Saves all collected profile data from the {@link ProfileUpdateState} to the user's
     * {@link UserPhysicalData} entity in the database. This method is typically called
     * at the end of the "all attributes" update flow.
     *
     * @param absSender The {@link AbsSender} instance for sending messages.
     * @param chatId The chat ID of the user.
     * @param state The current {@link ProfileUpdateState} containing the collected data.
     * @param user The {@link User} entity to which the physical data belongs.
     * @param lang The user's preferred {@link Language} for localization.
     */
    private void saveUpdatedProfileData(AbsSender absSender, Long chatId, ProfileUpdateState state, User user, Language lang) {
        UserPhysicalData physicalData = user.getUPD();
        if (physicalData == null) {
            physicalData = new UserPhysicalData();
            user.setUPD(physicalData);
        }

        physicalData.setSex(state.sex);
        physicalData.setWeight(state.weight);
        physicalData.setHeight(state.height);
        physicalData.setAge(state.age);
        physicalData.setBodyFatPercent(state.bodyFatPercent);
        physicalData.setPhysicalActivityLevel(state.activityLevel);
        physicalData.setMaingoal(state.mainGoal);

        userServiceInt.save(user);

        messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "message.profile.update.completed_all"));

        messageSender.sendMessage(absSender, chatId, userServiceInt.getProfileMessage(chatId));
    }

    /**
     * Saves a single updated attribute from the {@link ProfileUpdateState} to the user's
     * {@link UserPhysicalData} entity in the database. This method is called in the
     * "single attribute" update flow after each attribute's input is validated.
     * After saving, it returns the user to the attribute selection keyboard.
     *
     * @param absSender The {@link AbsSender} instance for sending messages.
     * @param chatId The chat ID of the user.
     * @param state The current {@link ProfileUpdateState} containing the updated attribute.
     * @param user The {@link User} entity to which the physical data belongs.
     * @param lang The user's preferred {@link Language} for localization.
     */
    private void saveSingleAttribute(AbsSender absSender, Long chatId, ProfileUpdateState state, User user, Language lang) {
        UserPhysicalData physicalData = user.getUPD();
        if (physicalData == null) {
            physicalData = new UserPhysicalData();
            user.setUPD(physicalData);
        }

        switch (state.getAttributeToUpdate()) {
            case "SEX" -> physicalData.setSex(state.sex);
            case "HEIGHT" -> physicalData.setHeight(state.height);
            case "WEIGHT" -> physicalData.setWeight(state.weight);
            case "AGE" -> physicalData.setAge(state.age);
            case "BODY_FAT" -> physicalData.setBodyFatPercent(state.bodyFatPercent);
            case "ACTIVITY_LEVEL" -> physicalData.setPhysicalActivityLevel(state.activityLevel);
            case "MAIN_GOAL" -> physicalData.setMaingoal(state.mainGoal);
        }

        userServiceInt.save(user);

        messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(lang, "message.attribute.updated") + " " + localizationService.getTranslation(lang, "profile.label." + state.getAttributeToUpdate().toLowerCase()));
        state.setCurrentStep("SELECT_ATTRIBUTE");
        sendAttributeSelectionKeyboard(absSender, chatId, lang);
    }
}