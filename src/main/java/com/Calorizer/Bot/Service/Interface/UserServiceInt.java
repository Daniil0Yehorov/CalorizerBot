package com.Calorizer.Bot.Service.Interface;

import com.Calorizer.Bot.Model.User;
import com.Calorizer.Bot.Model.UserPhysicalData;

/**
 * Interface defining service operations related to {@link User} and their {@link UserPhysicalData}.
 * This interface abstracts the business logic for user management and profile retrieval.
 */
public interface UserServiceInt {
    /**
     * Retrieves an existing user by chat ID or creates a new user if not found.
     * The new user will be initialized with default values (e.g., English language).
     *
     * @param chatId The Telegram chat ID of the user.
     * @return The existing or newly created {@link User} object.
     */
    User getOrCreateUser(Long chatId);

    /**
     * Generates and returns a formatted profile message for the given user.
     * This method handles access restrictions (e.g., paid accounts) and
     * initializes default physical data if it doesn't exist.
     *
     * @param chatId The Telegram chat ID of the user.
     * @return A localized string representing the user's profile or an access/empty profile message.
     */
    String getProfileMessage(Long chatId);

    /**
     * Saves or updates a {@link User} object in the database.
     *
     * @param user The {@link User} object to save.
     * @return The saved {@link User} object.
     */
    User save(User user);

    /**
     * Saves or updates a {@link UserPhysicalData} object in the database.
     *
     * @param userPhysicalData The {@link UserPhysicalData} object to save.
     * @return The saved {@link UserPhysicalData} object.
     */
    UserPhysicalData save(UserPhysicalData userPhysicalData);
    /**
     * Checks if the user's physical profile data is complete.
     * A profile is considered complete if all essential fields in {@link UserPhysicalData}
     * are non-null and have valid (non-zero or non-negative as appropriate) values.
     *
     * @param user The {@link User} object whose profile completeness is to be checked.
     * @return True if the user's physical profile data is complete, false otherwise.
     */
    boolean isUserProfileComplete(User user);
}
