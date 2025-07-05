package com.Calorizer.Bot.Model;

import com.Calorizer.Bot.Model.Enum.MainGoal;
import com.Calorizer.Bot.Model.Enum.PhysicalActivityLevel;
import com.Calorizer.Bot.Model.Enum.Sex;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.*;

/**
 * Represents the physical data (sex, weight, height, age, activity level, body fat, main goal)
 * of a user. This entity is closely linked to the {@link User} entity via a one-to-one relationship.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "User_Physical_Data")
public class UserPhysicalData {
    // в /profile ведет пользователь все данные, а при калькуляции может их заюзать вместо ввода
    // таблица будет доступна не бесплатно, ибо на ее основе будут считаться текущие запити клиента и будет формироваться
    //рекомендации еды завтрак,обед,ужин. Тоесть доступ до /profile только после true  в payedAcc

    /**
     * The primary key for this entity, which also serves as a foreign key
     * to the associated {@link User} (chat ID).
     */
    @Id
    @Column(name = "UserID")
    private Long id;

    /**
     * One-to-one relationship with the {@link User} entity.
     * {@code @MapsId} indicates that the primary key of this entity is mapped
     * from the primary key of the owning side of the relationship (the User entity).
     * {@code @JoinColumn} specifies the foreign key column in this table that links to the User table.
     */
    @OneToOne
    @MapsId
    @JoinColumn(name = "UserID", referencedColumnName = "chatId")
    private User user;

    /**
     * The user's biological sex. Stored as a string in the database.
     */
    @Enumerated(EnumType.STRING)
    private Sex sex;

    /**
     * The user's physical activity level. Stored as a string in the database.
     */
    @Enumerated(EnumType.STRING)
    private PhysicalActivityLevel physicalActivityLevel;

    /**
     * The user's weight in kilograms.
     */
    private double weight;

    /**
     * The user's height in centimeters.
     */
    private double height;

    /**
     * The user's age in years.
     */
    private int age;

    /**
     * The user's main fitness goal. Stored as a string in the database.
     */
    @Enumerated(EnumType.STRING)
    private MainGoal maingoal;

    /**
     * The user's body fat percentage.
     */
    private double bodyFatPercent;

}
