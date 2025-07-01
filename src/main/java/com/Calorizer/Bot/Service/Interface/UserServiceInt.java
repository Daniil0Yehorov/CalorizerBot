package com.Calorizer.Bot.Service.Interface;

import com.Calorizer.Bot.Model.User;
import com.Calorizer.Bot.Model.UserPhysicalData;

public interface UserServiceInt {
    User getOrCreateUser(Long chatId);

    String getProfileMessage(Long chatId);

    User save(User user);

    UserPhysicalData save(UserPhysicalData userPhysicalData);

    //UserPhysicalData update(**);

}
