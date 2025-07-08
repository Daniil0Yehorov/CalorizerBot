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
import org.springframework.stereotype.Service;

/**
 * Implementation of the {@link UserServiceInt} interface.
 * Provides business logic for managing users and their physical data,
 * including creation, retrieval, and profile message generation.
 */
@Service
public class UserServiceImpl implements UserServiceInt {

    private final LocalizationService localizationService;

    private final UserRepository userRepository;

    private final UserPhysicalDataRepository userPhysicalDataRepository;

    /**
     * Constructor for dependency injection. Spring automatically provides instances of the required repositories and services.
     *
     * @param localizationService Service for retrieving localized strings.
     * @param userRepository Repository for {@link User} entities.
     * @param userPhysicalDataRepository Repository for {@link UserPhysicalData} entities.
     */
    UserServiceImpl(LocalizationService localizationService,UserRepository userRepository, UserPhysicalDataRepository userPhysicalDataRepository){
        this.localizationService=localizationService;
        this.userRepository=userRepository;
        this.userPhysicalDataRepository=userPhysicalDataRepository;
    }

    /**
     * {@inheritDoc}
     * Retrieves an existing user by chat ID or creates a new user if not found.
     * New users are initialized with a default language (English) and a test payment status.
     * This method is transactional, ensuring atomicity of the operation.
     */
    @Override
    @Transactional
    public User getOrCreateUser(Long chatId) {
        return userRepository.findByChatId(chatId).orElseGet(() -> {
            User newUser = new User();
            newUser.setChatId(chatId);
            newUser.setLanguage(Language.English);
            //if else just for test /profile rn
            if(chatId==642196846 || chatId==755032378){newUser.setPayedAcc(true);}
            else {newUser.setPayedAcc(false);}
            return userRepository.save(newUser);
        });
    }

    /**
     * {@inheritDoc}
     * Generates and returns a formatted profile message for the given user.
     * Handles cases for non-existent users, non-paid accounts, and empty physical data profiles.
     * This method is transactional only if called from a non-transactional context. If called
     * from an existing transaction, it will join that transaction.
     */
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

    /**
     * {@inheritDoc}
     * Saves a {@link User} entity. This operation is managed by Spring Data JPA.
     */
    @Override
    public User save(User  user) {
        return userRepository.save(user);
    }

    /**
     * {@inheritDoc}
     * Saves a {@link UserPhysicalData} entity. This operation is managed by Spring Data JPA.
     */
    @Override
    public UserPhysicalData save(UserPhysicalData  data) {
        return userPhysicalDataRepository.save(data);
    }

    /**
     * Builds a formatted string containing the user's physical profile details.
     * All labels and units are localized based on the provided language.
     *
     * @param profile The {@link UserPhysicalData} object containing the profile details.
     * @param lang The {@link Language} for localization.
     * @return A formatted string representing the user's physical profile.
     */
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

    /**
     * Provides the correct localized suffix for age (e.g., "year", "years", "годы", "лет" etc.)
     * based on the number and target language. Handles pluralization rules for different languages.
     *
     * @param age The age of the user.
     * @param lang The target {@link Language} for localization.
     * @return The localized age suffix string.
     */
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
