package com.Calorizer.Bot.Repository;

import com.Calorizer.Bot.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByChatId(long chatId);
}
