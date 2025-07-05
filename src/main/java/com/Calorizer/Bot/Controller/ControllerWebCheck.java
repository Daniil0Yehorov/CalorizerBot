package com.Calorizer.Bot.Controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web controller to provide a simple web page, primarily for deployment health checks
 * or informational purposes (e.g., on platforms like Heroku).
 * It demonstrates basic Spring MVC functionality.
 */
@Controller
public class ControllerWebCheck {
    @Value("${server.port}")
    private String serverPort;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("serverPort", serverPort);
        return "index";
    }

}
