package com.Calorizer.Bot.Service;

import com.Calorizer.Bot.Model.Enum.Language;
import com.Calorizer.Bot.Model.User;
import com.Calorizer.Bot.Model.UserPhysicalData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for generating nutrition recommendations using the Gemini AI API.
 * It constructs a detailed prompt based on user's physical data and goals,
 * handles API communication, response parsing, and implements a cooldown mechanism
 * to prevent excessive API calls.
 */
@Service
@PropertySource("application.properties")
public class NutritionRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(NutritionRecommendationService.class);

    private final WebClient webClient;
    private final LocalizationService localizationService;

    @Value("${ai.url}")
    private String geminiApiFullUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    // Map to store the last request time for each user to implement a cooldown.
    // ConcurrentHashMap is used for thread safety in a multi-user environment.
    // Cooldown period in milliseconds for AI recommendations (30 seconds).
    private final Map<Long, Long> lastRequestTime = new ConcurrentHashMap<>();
    private static final long REQUEST_COOLDOWN_MS = 30 * 1000;

    private final ObjectMapper objectMapper;

    /**
     * Constructs a new NutritionRecommendationService.
     * Dependencies are injected by Spring.
     *
     * @param webClientBuilder Builder for creating a WebClient instance.
     * @param localizationService Service for retrieving localized messages.
     */
    public NutritionRecommendationService(WebClient.Builder webClientBuilder, LocalizationService localizationService) {
        this.webClient = webClientBuilder.build();
        this.localizationService = localizationService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Builds the comprehensive prompt string to be sent to the Gemini AI.
     * The prompt includes user's calculated calorie needs (from various methods),
     * their detailed physical profile, additional requirements, and specific instructions
     * for Gemini regarding the output format and task. The prompt is localized to the user's language.
     *
     * @param user The {@link User} object containing physical data and language preference.
     * @param duration A string indicating the duration for which the recommendation is requested (e.g., "for a week").
     * @param additionalRequirements Any specific additional requirements provided by the user.
     * @return A formatted string representing the prompt for the Gemini AI.
     */
    private String buildPrompt(User user, String duration, String additionalRequirements) {
        StringBuilder prompt = new StringBuilder();
        Language userLanguage = user.getLanguage();
        UserPhysicalData upd = user.getUPD();

        FullReportByMethods calorieReport = new FullReportByMethods(
                upd.getSex(),
                upd.getWeight(),
                upd.getHeight(),
                upd.getAge(),
                upd.getBodyFatPercent(),
                upd.getPhysicalActivityLevel(),
                upd.getMaingoal()
        );
        Map<String, Double> calculatedCalories = calorieReport.getResults();

        prompt.append(localizationService.getTranslation(userLanguage, "recommendation.prompt.main_request_prefix"))
                .append(" ")
                .append(duration)
                .append(localizationService.getTranslation(userLanguage, "recommendation.prompt.main_request_suffix"))
                .append(":\n\n");

        prompt.append(localizationService.getTranslation(userLanguage, "recommendation.prompt.calculated_calories_intro"))
                .append(":\n");

        for (Map.Entry<String, Double> entry : calculatedCalories.entrySet()) {
            String methodName = localizationService.getMethodTranslation(userLanguage, entry.getKey());
            prompt.append(String.format("- %s: %.2f kcal\n", methodName, entry.getValue()));
        }
        prompt.append("\n");

        String ageLabel = localizationService.getTranslation(userLanguage, "profile.label.age");
        String sexLabel = localizationService.getTranslation(userLanguage, "profile.label.sex");
        String heightLabel = localizationService.getTranslation(userLanguage, "profile.label.height");
        String weightLabel = localizationService.getTranslation(userLanguage, "profile.label.weight");
        String activityLevelLabel = localizationService.getTranslation(userLanguage, "profile.label.activity_level");
        String mainGoalLabel = localizationService.getTranslation(userLanguage, "profile.label.main_goal");
        String bodyFatLabel = localizationService.getTranslation(userLanguage, "profile.label.body_fat");
        String cmUnit = localizationService.getTranslation(userLanguage, "unit.height");
        String kgUnit = localizationService.getTranslation(userLanguage, "unit.weight");
        String additionalReqLabel = localizationService.getTranslation(userLanguage, "recommendation.prompt.additional_req_label");
        String yearsUnit;

        switch (userLanguage) {
            case Russian:
                yearsUnit = localizationService.getTranslation(userLanguage, "unit.age.year_ru_5_0");
                break;
            case Ukrainian:
                yearsUnit = localizationService.getTranslation(userLanguage, "unit.age.year_ua_5_0");
                break;
            case German:
                yearsUnit = localizationService.getTranslation(userLanguage, "unit.age.year_de");
                break;
            case English:
                yearsUnit = localizationService.getTranslation(userLanguage, "unit.age.year_en");
                break;
            default:
                yearsUnit = localizationService.getTranslation(userLanguage, "unit.age.year_en");
                break;
        }

        prompt.append(localizationService.getTranslation(userLanguage, "recommendation.prompt.user_profile_intro"))
                .append(":\n");

        prompt.append(ageLabel).append(": ").append(upd.getAge()).append(" ").append(yearsUnit).append("\n");
        String sexTranslated = localizationService.getTranslation(userLanguage, "enum.sex." + upd.getSex().name().toLowerCase());
        prompt.append(sexLabel).append(": ").append(sexTranslated).append("\n");
        prompt.append(heightLabel).append(": ").append(upd.getHeight()).append(" ").append(cmUnit).append("\n");
        prompt.append(weightLabel).append(": ").append(upd.getWeight()).append(" ").append(kgUnit).append("\n");
        String activityTranslated = localizationService.getTranslation(userLanguage, "enum.activity." + upd.getPhysicalActivityLevel().name().toLowerCase());
        prompt.append(activityLevelLabel).append(": ").append(activityTranslated).append("\n");
        String goalTranslated = localizationService.getTranslation(userLanguage, "enum.goal." + upd.getMaingoal().name().toLowerCase());
        prompt.append(mainGoalLabel).append(": ").append(goalTranslated).append("\n");

        if (upd.getBodyFatPercent() > 0) {
            prompt.append(bodyFatLabel).append(": ").append(upd.getBodyFatPercent()).append(" %\n");
        }
        prompt.append("\n");

        if (additionalRequirements != null && !additionalRequirements.trim().isEmpty()) {
            prompt.append(additionalReqLabel).append(": ").append(additionalRequirements).append("\n\n");
        }

        String finalGeminiInstruction = localizationService.getTranslation(userLanguage, "recommendation.prompt.gemini_task_instruction_final")
                .replace("{0}", duration)
                .replace("{1}", goalTranslated);

        prompt.append(finalGeminiInstruction);
        prompt.append("\n\n");

        prompt.append(localizationService.getTranslation(userLanguage, "recommendation.prompt.output_format_instructions_specific"));

        log.info("Sending prompt to Gemini for user {}: \n{}", user.getChatId(), prompt);
        return prompt.toString();
    }

    /**
     * Requests a nutrition recommendation from the Gemini AI.
     * This method first checks for a cooldown period to prevent abuse.
     * It then validates the user's profile completeness before building the prompt
     * and making the API call. It handles successful responses by parsing the AI's text output,
     * and manages various error scenarios (cooldown, incomplete profile, API communication issues, parsing errors).
     *
     * @param user The {@link User} for whom the recommendation is requested.
     * @param duration A string indicating the desired duration of the recommendation (e.g., "for a week").
     * @param additionalRequirements Any extra details or constraints for the recommendation.
     * @return A {@link Mono<String>} emitting the AI-generated recommendation text,
     * or an error message if the request fails or is on cooldown.
     */
    public Mono<String> getNutritionRecommendation(User user, String duration, String additionalRequirements) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastRequestTime.get(user.getChatId());
        Language userLanguage = user.getLanguage();

         if (lastTime != null && (currentTime - lastTime) < REQUEST_COOLDOWN_MS) {
            long remainingSeconds = (REQUEST_COOLDOWN_MS - (currentTime - lastTime)) / 1000;
            String cooldownMessage = localizationService.getTranslation(userLanguage, "error.recommendation_cooldown");
            return Mono.just(cooldownMessage.replace("{0}", String.valueOf(remainingSeconds)));
        }

         UserPhysicalData upd = user.getUPD();
        if (upd == null || upd.getMaingoal() == null || upd.getSex() == null ||
                upd.getPhysicalActivityLevel() == null ||
                upd.getAge() == 0 ||
                upd.getHeight() == 0 ||
                upd.getWeight() == 0.0) {

             return Mono.just(localizationService.getTranslation(userLanguage, "error.profile_not_complete_for_ai_recommendations"));
        }

        lastRequestTime.put(user.getChatId(), currentTime);

         String prompt = buildPrompt(user, duration, additionalRequirements);

        log.info("Sending prompt to Gemini for user {}: {}", user.getChatId(), prompt);

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        Map<String, Object> content = new HashMap<>();
        content.put("parts", new Object[]{part});
        content.put("role", "user");

        requestBody.put("contents", new Object[]{content});

        return webClient.post()
                .uri(geminiApiFullUrl)
                .header("X-goog-api-key", geminiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        JsonNode root = objectMapper.readTree(response);
                         JsonNode textNode = root.path("candidates").get(0).path("content").path("parts").get(0).path("text");
                        if (textNode != null) {
                            return textNode.asText();
                        } else {
                             log.error("Gemini response did not contain expected text content for user {}. Full response: {}", user.getChatId(), response);
                            return localizationService.getTranslation(userLanguage, "error.ai_generation_failed");
                        }
                    } catch (Exception e) {
                        log.error("Failed to parse Gemini response for user {}. Response: {}", user.getChatId(), response, e);
                        return localizationService.getTranslation(userLanguage, "error.ai_generation_failed");
                    }
                })
                 .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("WebClient error during Gemini API call for user {} (status {}): {}. Response body: {}",
                            user.getChatId(), ex.getStatusCode(), ex.getMessage(), ex.getResponseBodyAsString(), ex);
                    return Mono.just(localizationService.getTranslation(userLanguage, "error.ai_communication_error"));
                })
                 .onErrorResume(Exception.class, ex -> {
                    log.error("An unexpected error occurred during Gemini API call for user {}: {}", user.getChatId(), ex.getMessage(), ex);
                    return Mono.just(localizationService.getTranslation(userLanguage, "error.generic"));
                });
    }
}