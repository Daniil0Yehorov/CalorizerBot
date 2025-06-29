package com.Calorizer.Bot.MainBot.CalculateMethods;


import com.Calorizer.Bot.Model.Enum.MainGoal;
import com.Calorizer.Bot.Model.Enum.PhysicalActivityLevel;
import com.Calorizer.Bot.Model.Enum.Sex;

import java.util.HashMap;
import java.util.Map;

public class FullReportByMethods {

    private Sex sex;
    private PhysicalActivityLevel physicalActivityLevel;
    private double weight;
    private double height;
    private int age;
    private MainGoal maingoal;
    private double bodyFatPercent;
    private Map<String, Double> methodResults = new HashMap<>();

    public FullReportByMethods(Sex sex, double weight, double height, int age, double bodyFatPercent,
                               PhysicalActivityLevel activityLevel, MainGoal maingoal) {
        this.sex = sex;
        this.weight = weight;
        this.height = height;
        this.age = age;
        this.bodyFatPercent = bodyFatPercent;
        this.physicalActivityLevel = activityLevel;
        this.maingoal = maingoal;

        calculateAll();
    }

    private void calculateAll() {
        methodResults.put("Harris-Benedict", calculateHarrisBenedict());
        methodResults.put("Mifflin-St Jeor", calculateMifflinStJeor());
        methodResults.put("Katch-McArdle", calculateKatchMcArdle());
        methodResults.put("Tom Venuto", calculateTomVenuto());
    }

    private double calculateHarrisBenedict(){
        if (sex == Sex.MALE) {
            return (88.362 + 13.397 * weight + 4.799 * height - 5.677 * age) * physicalActivityLevel.getFactor();
        } else {
            return (447.593 + 9.247 * weight + 3.098 * height - 4.330 * age) * physicalActivityLevel.getFactor();
        }
    }

    private double calculateMifflinStJeor(){
        if (sex == Sex.MALE) {
            return (10 * weight + 6.25 * height - 5 * age + 5) * physicalActivityLevel.getFactor();
        } else {
            return (10 * weight + 6.25 * height - 5 * age - 161) * physicalActivityLevel.getFactor();
        }
    }

    private double calculateKatchMcArdle(){
        double mm = weight * (1 - bodyFatPercent / 100);
        return 370 + (21.6 * mm);
    }

    private double calculateTomVenuto()
    {
        double bmr = calculateMifflinStJeor();
        double multiplier = maingoal.getFactor();
        return bmr * multiplier;
    }

    public Map<String, Double> getResults() {
        return methodResults;
    }
}
