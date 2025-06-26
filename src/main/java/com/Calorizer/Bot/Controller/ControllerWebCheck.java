package com.Calorizer.Bot.Controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
