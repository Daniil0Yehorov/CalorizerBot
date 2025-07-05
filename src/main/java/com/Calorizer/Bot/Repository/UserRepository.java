package com.Calorizer.Bot.Repository;

import com.Calorizer.Bot.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for {@link User} entities.
 * Extends {@link JpaRepository} to provide standard CRUD operations
 * and additional custom query methods for User objects.
 * The primary key type for User is Long (chatId).
 */
public interface UserRepository extends JpaRepository<User,Long> {
    /**
     * Finds a {@link User} entity by their Telegram chat ID.
     * Spring Data JPA automatically generates the query based on the method name.
     *
     * @param chatId The unique Telegram chat ID of the user.
     * @return An {@link Optional} containing the User if found, or an empty Optional if no user exists with the given chat ID.
     */
    Optional<User> findByChatId(long chatId);
}
