package com.Calorizer.Bot.Model;

import com.Calorizer.Bot.Model.Enum.MainGoal;
import com.Calorizer.Bot.Model.Enum.PhysicalActivityLevel;
import com.Calorizer.Bot.Model.Enum.Sex;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.*;


@Getter
@Setter
@NoArgsConstructor
@Entity
public class UserPhysicalData {
    // в /profile ведет пользователь все данные, а при калькуляции может их заюзать вместо ввода
    // таблица будет доступна не бесплатно, ибо на ее основе будут считаться текущие запити клиента и будет формироваться
    //рекомендации еды завтрак,обед,ужин. Тоесть доступ до /profile только после true  в payedAcc
    @Id
    @Column(name = "UserID")
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "UserID", referencedColumnName = "chatId")
    private User user;

    @Enumerated(EnumType.STRING)
    private Sex sex;

     @Version
     private Long version;

    @Enumerated(EnumType.STRING)
    private PhysicalActivityLevel physicalActivityLevel;

    private double weight;

    private double height;

    private int age;

    @Enumerated(EnumType.STRING)
    private MainGoal maingoal;

    private double bodyFatPercent;

}
