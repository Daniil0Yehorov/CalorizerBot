package com.Calorizer.Bot.Model;

import com.Calorizer.Bot.Model.Enum.Language;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name="User")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long chatId;
    private boolean payedAcc;
    @Enumerated(EnumType.STRING)
    private Language language;
}
