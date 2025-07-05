package com.Calorizer.Bot.Model;

import com.Calorizer.Bot.Model.Enum.Language;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a user of the bot.
 * This entity stores basic user information, including their Telegram chat ID,
 * payment status, and preferred language. It also has a one-to-one relationship
 * with {@link UserPhysicalData} for storing physical attributes.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name="User")
public class User {

    /**
     * The unique chat ID of the Telegram user.
     * This also serves as the primary key for the User table.
     */
    @Id
    private Long chatId;

    /**
     * Indicates whether the user has a paid account.
     */
    @Column(nullable = false)
    private boolean payedAcc;
    @Enumerated(EnumType.STRING)

    /**
     * The preferred language of the user.
     * Stored as a string in the database for flexibility.
     */
    @Column(nullable = false)
    private Language language;

    /**
     * One-to-one relationship with {@link UserPhysicalData}.
     * This field holds the physical data associated with this user.
     * CascadeType.ALL ensures operations (persist, merge, remove) on User also apply to UPD.
     * orphanRemoval = true ensures that if a UPD is disassociated from a User, it's deleted.
     * FetchType.LAZY means UPD data is loaded only when accessed.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserPhysicalData UPD;
}
