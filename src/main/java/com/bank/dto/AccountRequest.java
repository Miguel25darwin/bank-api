package com.bank.dto;

import com.bank.model.Account;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AccountRequest {

    @NotBlank(message = "Le nom du proprietaire est obligatoire")
    private String ownerName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format email invalide")
    private String email;

    @NotNull(message = "Le type de compte est obligatoire")
    private Account.AccountType accountType;

    @Pattern(regexp = "^[A-Za-z]{3}$", message = "La devise doit être un code ISO 4217 à 3 lettres (ex: EUR, USD, GBP)")
    @Schema(example = "EUR", description = "Code devise ISO 4217 (3 lettres). Optionnel, défaut : EUR")
    private String currency = "EUR";
}
