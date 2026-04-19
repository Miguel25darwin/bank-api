package com.bank.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur pour gérer la page d'accueil.
 * Redirige automatiquement vers Swagger UI pour une meilleure expérience
 * utilisateur.
 */
@Controller
public class IndexController {

    @GetMapping("/")
    public String index() {
        return "redirect:/swagger-ui.html";
    }
}
