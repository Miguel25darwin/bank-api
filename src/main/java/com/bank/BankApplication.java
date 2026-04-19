package com.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée principal de l'application Bank API.
 *
 * <p>
 * {@code @SpringBootApplication} active automatiquement le scan de composants
 * sur le package {@code com.bank} et tous ses sous-packages :
 * {@code com.bank.controller}, {@code com.bank.service},
 * {@code com.bank.repository},
 * {@code com.bank.model}, {@code com.bank.dto}, {@code com.bank.exception}.
 * </p>
 *
 * <p>
 * URLs utiles (mode développement) :
 * </p>
 * <ul>
 * <li>Swagger UI : <a href=
 * "http://localhost:8080/swagger-ui.html">http://localhost:8080/swagger-ui.html</a></li>
 * <li>Console H2 : <a href=
 * "http://localhost:8080/h2-console">http://localhost:8080/h2-console</a></li>
 * </ul>
 */
@SpringBootApplication
public class BankApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankApplication.class, args);
    }
}