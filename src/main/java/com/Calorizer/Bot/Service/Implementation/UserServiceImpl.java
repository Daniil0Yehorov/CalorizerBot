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
import com.Calorizer.Bot.Service.LocalizationService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserServiceInt {

    @Autowired
    private LocalizationService localizationService;

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
            return localizationService.getTranslation(user.getLanguage(), "profile.access_paid_only");
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
            String greeting = localizationService.getTranslation(user.getLanguage(), "profile.greeting_new");
            String fillProfileMsg = localizationService.getTranslation(user.getLanguage(), "profile.empty_message");
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
        String greeting = localizationService.getTranslation(lang, "profile.greeting_existing");

        String sexLabel = localizationService.getTranslation(lang, "profile.label.sex");
        String activityLabel = localizationService.getTranslation(lang, "profile.label.activity_level");
        String weightLabel = localizationService.getTranslation(lang, "profile.label.weight");
        String heightLabel = localizationService.getTranslation(lang, "profile.label.height");
        String ageLabel = localizationService.getTranslation(lang, "profile.label.age");
        String goalLabel = localizationService.getTranslation(lang, "profile.label.main_goal");
        String bodyFatLabel = localizationService.getTranslation(lang, "profile.label.body_fat");

        StringBuilder sb = new StringBuilder(greeting);

        sb.append(String.format("%s: %s\n", sexLabel, profile.getSex() != null ?
                localizationService.getSexTranslation(lang, profile.getSex()) :
                localizationService.getTranslation(lang, "common.not_specified")));

        sb.append(String.format("%s: %s\n", activityLabel, profile.getPhysicalActivityLevel() != null ?
                localizationService.getPhysicalActivityLevelTranslation(lang, profile.getPhysicalActivityLevel()) :
                localizationService.getTranslation(lang, "common.not_specified")));

        sb.append(String.format("%s: %.1f %s\n", weightLabel, profile.getWeight(), localizationService.getTranslation(lang, "unit.weight")));
        sb.append(String.format("%s: %.1f %s\n", heightLabel, profile.getHeight(), localizationService.getTranslation(lang, "unit.height")));
        sb.append(String.format("%s: %d %s\n", ageLabel, profile.getAge(), ageSuffix(profile.getAge(), lang)));

        sb.append(String.format("%s: %s\n", goalLabel, profile.getMaingoal() != null ?
                localizationService.getMainGoalTranslation(lang, profile.getMaingoal()) :
                localizationService.getTranslation(lang, "common.not_specified")));

        sb.append(String.format("%s: %.1f%%\n", bodyFatLabel, profile.getBodyFatPercent()));

        return sb.toString();
    }

    private String ageSuffix(int age, Language lang) {
        int mod10 = age % 10;
        int mod100 = age % 100;

        String key;
        switch (lang) {
            case Ukrainian:
                if (mod10 == 1 && mod100 != 11) key = "unit.age.year_ua_1";
                else if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) key = "unit.age.year_ua_2_4";
                else key = "unit.age.year_ua_5_0";
                break;
            case Russian:
                if (mod10 == 1 && mod100 != 11) key = "unit.age.year_ru_1";
                else if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) key = "unit.age.year_ru_2_4";
                else key = "unit.age.year_ru_5_0";
                break;
            case German:
                key = "unit.age.year_de";
                break;
            default:
                key = "unit.age.year_en";
                break;
        }
        return localizationService.getTranslation(lang, key);
    }

}
