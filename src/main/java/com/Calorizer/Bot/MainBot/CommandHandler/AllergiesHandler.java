package com.Calorizer.Bot.MainBot.CommandHandler;

import com.Calorizer.Bot.MainBot.CallbackCallback.CallbackHandler;
import com.Calorizer.Bot.Model.Enum.Allergen;
import com.Calorizer.Bot.Model.Enum.Language;
import com.Calorizer.Bot.Model.User;
import com.Calorizer.Bot.Service.Interface.UserServiceInt;
import com.Calorizer.Bot.Service.LocalizationService;
import com.Calorizer.Bot.Service.MessageSender;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles commands and callbacks related to user allergy management.
 * This class allows users to view and toggle their known allergens,
 * which are then used for personalized meal recommendations.
 *
 * <p>Access to this feature is restricted to premium users.</p>
 */
@Component
public class AllergiesHandler implements CommandHandler, CallbackHandler {

    private static final String ALLERGY_CALLBACK_PREFIX = "ALLERGY_TOGGLE:";
    private final UserServiceInt userService;
    private final LocalizationService localizationService;
    private final MessageSender messageSender;

    /**
     * Constructs an AllergiesHandler.
     *
     * @param userService The service for managing user data.
     * @param localizationService The service for retrieving localized messages.
     * @param messageSender The service for sending and editing messages to Telegram.
     */
    public AllergiesHandler(UserServiceInt userService, LocalizationService localizationService, MessageSender messageSender) {
        this.userService = userService;
        this.localizationService = localizationService;
        this.messageSender = messageSender;
    }

    /**
     * Determines if this handler supports the given command text or callback data.
     * It supports the "/allergy" command, callback data starting with {@link #ALLERGY_CALLBACK_PREFIX},
     * and localized versions of the "allergies" button text across all supported languages.
     *
     * @param commandText The incoming command text or callback data.
     * @return {@code true} if this handler can process the input, {@code false} otherwise.
     */
    @Override
    public boolean supports(String commandText) {
        if ("/allergy".equals(commandText) || commandText.startsWith(ALLERGY_CALLBACK_PREFIX)) {
            return true;
        }
        return localizationService.getTranslation(Language.English, "button.command.allergies").equals(commandText) ||
                localizationService.getTranslation(Language.Russian, "button.command.allergies").equals(commandText) ||
                localizationService.getTranslation(Language.Ukrainian, "button.command.allergies").equals(commandText) ||
                localizationService.getTranslation(Language.German, "button.command.allergies").equals(commandText);

    }

    /**
     * Handles incoming updates (messages or callback queries) related to allergen management.
     * It first checks if the user has a premium account. If not, an access denied message is sent.
     * For premium users, it either sends the allergen selection menu (for new commands)
     * or toggles the selected allergen (for callback queries).
     *
     * @param absSender The {@link AbsSender} instance (the bot itself) used to execute API methods.
     * @param update The incoming {@link Update} object from Telegram.
     */
    @Override
    public void handle(AbsSender absSender, Update update) {
        long chatId = update.hasMessage() ? update.getMessage().getChatId() : update.getCallbackQuery().getMessage().getChatId();
        User user = userService.getOrCreateUser(chatId);

        if (!user.isPayedAcc()) {
            messageSender.sendMessage(absSender, chatId, localizationService.getTranslation(user.getLanguage(), "error.allergies_access_paid_only"));
            return;
        }
        if (update.hasMessage()) {
            sendAllergiesMenu(absSender, chatId, user);
        } else if (update.hasCallbackQuery()) {

            String callbackData = update.getCallbackQuery().getData();
            if (callbackData.startsWith(ALLERGY_CALLBACK_PREFIX)) {
                toggleAllergen(absSender, update, user);
            }
        }
    }

    /**
     * Sends the initial message with the inline keyboard for allergen selection.
     * This menu allows the user to see their currently selected allergens and toggle them.
     *
     * @param absSender The {@link AbsSender} instance.
     * @param chatId The chat ID to send the message to.
     * @param user The {@link User} whose allergen settings are being managed.
     */
    private void sendAllergiesMenu(AbsSender absSender, long chatId, User user) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(localizationService.getTranslation(user.getLanguage(), "allergies.menu_intro"));
        message.setReplyMarkup(buildAllergiesKeyboard(user));
        messageSender.sendMessage(absSender, message);
    }

    /**
     * Builds the {@link InlineKeyboardMarkup} for the allergen selection menu.
     * Each allergen is represented by a button. Selected allergens are marked with a checkmark (✅).
     *
     * @param user The {@link User} for whom the keyboard is being built, used to retrieve current allergens and language.
     * @return An {@link InlineKeyboardMarkup} containing buttons for all {@link Allergen} values.
     */
    private InlineKeyboardMarkup buildAllergiesKeyboard(User user) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<Allergen> userAllergens = user.getUPD().getAllergens();

        for (Allergen allergen : Allergen.values()) {
            String buttonText = localizationService.getTranslation(user.getLanguage(), "allergen." + allergen.name().toLowerCase());
            boolean isSelected = userAllergens != null && userAllergens.contains(allergen);
            if (isSelected) {
                buttonText = "✅ " + buttonText;
            }

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(buttonText);
            button.setCallbackData(ALLERGY_CALLBACK_PREFIX + allergen.name());
            rows.add(List.of(button));
        }

        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Toggles the selection status of an allergen based on a user's callback query.
     * If the allergen was selected, it's deselected; otherwise, it's selected.
     * The user's {@link com.Calorizer.Bot.Model.UserPhysicalData} is updated and the allergen menu message is re-edited
     * to reflect the change.
     *
     * @param absSender The {@link AbsSender} instance.
     * @param update The incoming {@link Update} object containing the callback query.
     * @param user The {@link User} whose allergen settings are being updated.
     */
    private void toggleAllergen(AbsSender absSender, Update update, User user) {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        String callbackData = update.getCallbackQuery().getData();
        String allergenName = callbackData.substring(ALLERGY_CALLBACK_PREFIX.length());
        Allergen allergen = Allergen.valueOf(allergenName);

        List<Allergen> userAllergens = user.getUPD().getAllergens();

        if (userAllergens == null) {
            userAllergens = new ArrayList<>();
        } else {
            userAllergens = new ArrayList<>(userAllergens);
        }

        if (userAllergens.contains(allergen)) {
            userAllergens.remove(allergen);
        } else {
            userAllergens.add(allergen);
        }

        user.getUPD().setAllergens(userAllergens);

        userService.save(user);

        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(String.valueOf(chatId));
        editMessage.setMessageId(messageId);
        editMessage.setText(localizationService.getTranslation(user.getLanguage(), "allergies.menu_intro"));
        editMessage.setReplyMarkup(buildAllergiesKeyboard(user));

        messageSender.editMessage(absSender, editMessage);
    }
}