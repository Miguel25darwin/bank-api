package com.bank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test de démarrage de l'application.
 * Vérifie que le contexte Spring Boot se charge sans erreur.
 */
@SpringBootTest
@DisplayName("BankApplication — Test de démarrage")
class BankApplicationTest {

    @Test
    @DisplayName("Le contexte Spring Boot se charge correctement")
    void contextLoads() {
        // Si ce test passe, toute la configuration Spring (JPA, Swagger, etc.)
        // est correcte et l'application peut démarrer.
    }
}
