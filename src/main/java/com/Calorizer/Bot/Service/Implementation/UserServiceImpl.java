package com.Calorizer.Bot.Service.Implementation;

import com.Calorizer.Bot.Model.Enum.Language;
import com.Calorizer.Bot.Model.Enum.MainGoal;
import com.Calorizer.Bot.Model.Enum.PhysicalActivityLevel;
import com.Calorizer.Bot.Model.Enum.Sex;
import com.Calorizer.Bot.Model.User;
import com.Calorizer.Bot.Model.UserPhysicalData;
import com.Calorizer.Bot.Repository.UserPhysicalDataRepository;
import com.Calorizer.Bot.Repository.UserRepository;
import com.Calorizer.Bot.Service.Interface.UserServiceInt;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserServiceInt {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPhysicalDataRepository userPhysicalDataRepository;

    @Override
    @Transactional
    public User getOrCreateUser(Long chatId) {
        return userRepository.findByChatId(chatId).orElseGet(() -> {
            User newUser = new User();
            newUser.setChatId(chatId);
            newUser.setLanguage(Language.English);
            //if else just for test /profile rn
            if(chatId==642196846){newUser.setPayedAcc(true);}
            else {newUser.setPayedAcc(true);}
            return userRepository.save(newUser);
        });
    }

    @Override
    @Transactional
    public String getProfileMessage(Long chatId) {
        User user = userRepository.findByChatId(chatId).orElse(null);
        if (user == null) {
            return "User not found.";
        }

        if (!user.isPayedAcc()) {
            return switch (user.getLanguage()) {
                case Ukrainian -> "Доступ до профілю доступний лише для платних користувачів.";
                case Russian -> "Доступ к профилю доступен только для платных пользователей.";
                case German -> "Der Zugriff auf das Profil ist nur für zahlende Benutzer verfügbar.";
                default -> "Profile access is available only for paid users.";
            };
        }

        if (!userPhysicalDataRepository.existsById(user.getChatId())) {
            UserPhysicalData userPhysicalData = new UserPhysicalData();
            userPhysicalData.setUser(user);
            user.setUPD(userPhysicalData);

            userPhysicalData.setSex(Sex.MALE);
            userPhysicalData.setPhysicalActivityLevel(PhysicalActivityLevel.ACTIVE);
            userPhysicalData.setWeight(75.5);
            userPhysicalData.setHeight(180.0);
            userPhysicalData.setAge(30);
            userPhysicalData.setMaingoal(MainGoal.WEIGHT_LOSS);
            userPhysicalData.setBodyFatPercent(15.3);

            userPhysicalDataRepository.save(userPhysicalData);
            String greeting = switch (user.getLanguage()) {
                case Ukrainian -> "Привіт, це ваші дані! 👋\n";
                case Russian -> "Привет, это ваши данные! 👋\n";
                case German -> "Hallo, das sind Ihre Daten! 👋\n";
                default -> "Hi, this is your data! 👋\n";
            };
            String fillProfileMsg = switch (user.getLanguage()) {
                case Ukrainian -> "Ваш профіль поки що порожній. Будь ласка, заповніть його.";
                case Russian -> "Ваш профиль пока что пустой. Пожалуйста, заполните его.";
                case German -> "Ihr Profil ist noch leer. Bitte füllen Sie es aus.";
                default -> "Your profile is currently empty. Please fill it out.";
            };
            return greeting + fillProfileMsg;
        }

        UserPhysicalData profile = userPhysicalDataRepository.findById(chatId).orElse(null);
        if (profile == null) {
            return "Profile not found.";
        }

        return buildProfileMessage(profile, user.getLanguage());
    }

    @Override
    public User save(User  user) {
        return userRepository.save(user);
    }

    @Override
    public UserPhysicalData save(UserPhysicalData  data) {
        return userPhysicalDataRepository.save(data);
    }

    private String buildProfileMessage(UserPhysicalData profile, Language lang) {
        String greeting = switch (lang) {
            case Ukrainian -> "Привіт, це ваші дані! 👋\n";
            case Russian -> "Привет, это ваши данные! 👋\n";
            case German -> "Hallo, das sind Ihre Daten! 👋\n";
            default -> "Hi, this is your data! 👋\n";
        };

        String sexLabel = switch (lang) {
            case Ukrainian -> "Стать";
            case Russian -> "Пол";
            case German -> "Geschlecht";
            default -> "Sex";
        };

        String activityLabel = switch (lang) {
            case Ukrainian -> "Рівень активності";
            case Russian -> "Уровень активности";
            case German -> "Aktivitätslevel";
            default -> "Physical Activity Level";
        };

        String weightLabel = switch (lang) {
            case Ukrainian -> "Вага";
            case Russian -> "Вес";
            case German -> "Gewicht";
            default -> "Weight";
        };

        String heightLabel = switch (lang) {
            case Ukrainian -> "Зріст";
            case Russian -> "Рост";
            case German -> "Größe";
            default -> "Height";
        };

        String ageLabel = switch (lang) {
            case Ukrainian -> "Вік";
            case Russian -> "Возраст";
            case German -> "Alter";
            default -> "Age";
        };

        String goalLabel = switch (lang) {
            case Ukrainian -> "Головна мета";
            case Russian -> "Главная цель";
            case German -> "Hauptziel";
            default -> "Main Goal";
        };

        String bodyFatLabel = switch (lang) {
            case Ukrainian -> "Відсоток жиру";
            case Russian -> "Процент жира";
            case German -> "Körperfettanteil";
            default -> "Body Fat Percentage";
        };

        StringBuilder sb = new StringBuilder(greeting);
        sb.append(String.format("%s: %s\n", sexLabel, profile.getSex() != null ? profile.getSex() : localizedNotSpecified(lang)));
        sb.append(String.format("%s: %s\n", activityLabel, profile.getPhysicalActivityLevel() != null ? profile.getPhysicalActivityLevel() : localizedNotSpecified(lang)));
        sb.append(String.format("%s: %.1f %s\n", weightLabel, profile.getWeight(), getWeightUnit(lang)));
        sb.append(String.format("%s: %.1f %s\n", heightLabel, profile.getHeight(), getHeightUnit(lang)));
        sb.append(String.format("%s: %d %s\n", ageLabel, profile.getAge(), ageSuffix(profile.getAge(), lang)));
        sb.append(String.format("%s: %s\n", goalLabel, profile.getMaingoal() != null ? profile.getMaingoal() : localizedNotSpecified(lang)));
        sb.append(String.format("%s: %.1f%%\n", bodyFatLabel, profile.getBodyFatPercent()));

        return sb.toString();
    }

    private String localizedNotSpecified(Language lang) {
        return switch (lang) {
            case Ukrainian -> "Не вказано";
            case Russian -> "Не указано";
            case German -> "Nicht angegeben";
            default -> "Not specified";
        };
    }

    private String ageSuffix(int age, Language lang) {
        int mod10 = age % 10;
        int mod100 = age % 100;

        switch (lang) {
            case Ukrainian:
                if (mod10 == 1 && mod100 != 11) return "рік";
                if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return "роки";
                return "років";

            case Russian:
                if (mod10 == 1 && mod100 != 11) return "год";
                if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return "года";
                return "лет";

            case German:
                return "Jahre";

            default:
                return "years";
        }
    }

    private String getWeightUnit(Language lang) {
        return switch (lang) {
            case Ukrainian, Russian -> "кг";
            case German -> "kg";
            default -> "kg";
        };
    }

    private String getHeightUnit(Language lang) {
        return switch (lang) {
            case Ukrainian, Russian -> "см";
            case German -> "cm";
            default -> "cm";
        };
    }

}
