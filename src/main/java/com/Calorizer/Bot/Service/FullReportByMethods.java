package com.Calorizer.Bot.Service;


import com.Calorizer.Bot.Model.Enum.MainGoal;
import com.Calorizer.Bot.Model.Enum.PhysicalActivityLevel;
import com.Calorizer.Bot.Model.Enum.Sex;

import java.util.HashMap;
import java.util.Map;

/**
 * A utility class to encapsulate the calculation of calorie recommendations
 * using various established methods (Harris-Benedict, Mifflin-St Jeor, Katch-McArdle, Tom Venuto).
 * It takes physical data as input and provides a map of method names to their calculated calorie results.
 */
public class FullReportByMethods {

    private Sex sex;
    private PhysicalActivityLevel physicalActivityLevel;
    private double weight;
    private double height;
    private int age;
    private MainGoal maingoal;
    private double bodyFatPercent;
    private Map<String, Double> methodResults = new HashMap<>();

    /**
     * Constructor for FullReportByMethods.
     * Takes all necessary physical data as input and immediately performs all calorie calculations.
     *
     * @param sex The user's biological sex.
     * @param weight The user's weight in kilograms.
     * @param height The user's height in centimeters.
     * @param age The user's age in years.
     * @param bodyFatPercent The user's body fat percentage.
     * @param activityLevel The user's physical activity level.
     * @param maingoal The user's main fitness goal.
     */
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

    /**
     * Performs all supported calorie calculation methods and stores their results
     * in the {@code methodResults} map.
     */
    private void calculateAll() {
        methodResults.put("Harris-Benedict", calculateHarrisBenedict());
        methodResults.put("Mifflin-St Jeor", calculateMifflinStJeor());
        methodResults.put("Katch-McArdle", calculateKatchMcArdle());
        methodResults.put("Tom Venuto", calculateTomVenuto());
    }

    /**
     * Calculates the Basal Metabolic Rate (BMR) and Total Daily Energy Expenditure (TDEE)
     * using the Harris-Benedict equation.
     *
     * @return The calculated daily calorie expenditure in kcal.
     */
    private double calculateHarrisBenedict(){
        if (sex == Sex.MALE) {
            return (88.362 + 13.397 * weight + 4.799 * height - 5.677 * age) * physicalActivityLevel.getFactor();
        } else {
            return (447.593 + 9.247 * weight + 3.098 * height - 4.330 * age) * physicalActivityLevel.getFactor();
        }
    }

    /**
     * Calculates the Basal Metabolic Rate (BMR) and Total Daily Energy Expenditure (TDEE)
     * using the Mifflin-St Jeor equation.
     *
     * @return The calculated daily calorie expenditure in kcal.
     */
    private double calculateMifflinStJeor(){
        if (sex == Sex.MALE) {
            return (10 * weight + 6.25 * height - 5 * age + 5) * physicalActivityLevel.getFactor();
        } else {
            return (10 * weight + 6.25 * height - 5 * age - 161) * physicalActivityLevel.getFactor();
        }
    }

    /**
     * Calculates the Basal Metabolic Rate (BMR) using the Katch-McArdle formula.
     * This method requires body fat percentage.
     *
     * @return The calculated BMR in kcal.
     */
    private double calculateKatchMcArdle(){
        double mm = weight * (1 - bodyFatPercent / 100);
        return 370 + (21.6 * mm);
    }

    /**
     * Calculates recommended daily calorie intake based on Mifflin-St Jeor BMR
     * and a factor derived from the user's {@link MainGoal}.
     *
     * @return The adjusted daily calorie intake in kcal.
     */
    private double calculateTomVenuto()
    {
        double bmr = calculateMifflinStJeor();
        double multiplier = maingoal.getFactor();
        return bmr * multiplier;
    }

    /**
     * Returns an unmodifiable map of all calculated calorie results.
     * The map keys are method names (String) and values are calorie amounts (Double).
     *
     * @return An unmodifiable {@link Map} of calculation results.
     */
    public Map<String, Double> getResults() {
        return methodResults;
    }
}
