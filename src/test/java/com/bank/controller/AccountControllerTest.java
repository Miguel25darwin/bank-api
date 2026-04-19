package com.bank.controller;

import com.bank.dto.AccountRequest;
import com.bank.dto.TransactionRequest;
import com.bank.model.Account.AccountType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration de la couche HTTP pour {@link AccountController}.
 *
 * <p>
 * Charge le contexte Spring complet avec H2 en mémoire.
 * Chaque test dispose d'une base de données propre grâce à
 * {@code DirtiesContext}.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("AccountController — Tests d'Intégration MockMvc")
class AccountControllerTest {

    private static final String BASE_URL = "/api/v1/accounts";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ================================================================
    // POST /api/v1/accounts — Créer un compte
    // ================================================================

    @Test
    @DisplayName("POST /accounts — 201 : compte créé avec les bons champs")
    void createAccount_validRequest_returns201() throws Exception {
        AccountRequest request = buildAccountRequest("Alice Dupont", "alice@example.com", AccountType.SAVINGS);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerName").value("Alice Dupont"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.accountNumber").exists())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("POST /accounts — 409 : email déjà utilisé")
    void createAccount_duplicateEmail_returns409() throws Exception {
        AccountRequest request = buildAccountRequest("Bob Martin", "bob@example.com", AccountType.CHECKING);
        String body = objectMapper.writeValueAsString(request);

        // Premier appel : succès
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        // Deuxième appel : conflit
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("POST /accounts — 400 : body invalide (champs manquants)")
    void createAccount_invalidRequest_returns400WithDetails() throws Exception {
        // Body vide — tous les champs annotés @NotBlank/@NotNull vont échouer
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").exists());
    }

    @Test
    @DisplayName("POST /accounts — 400 : email invalide")
    void createAccount_invalidEmail_returns400() throws Exception {
        AccountRequest request = buildAccountRequest("Charlie", "not-an-email", AccountType.CHECKING);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").exists());
    }

    // ================================================================
    // GET /api/v1/accounts — Lister tous les comptes
    // ================================================================

    @Test
    @DisplayName("GET /accounts — 200 : retourne une liste vide initialement")
    void getAllAccounts_empty_returns200EmptyList() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /accounts — 200 : retourne les comptes créés")
    void getAllAccounts_afterCreation_returnsAccounts() throws Exception {
        // Arrange : créer 2 comptes
        createAccountAndReturn("Diane", "diane@example.com", AccountType.SAVINGS);
        createAccountAndReturn("Eric", "eric@example.com", AccountType.BUSINESS);

        // Act & Assert
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].email",
                        containsInAnyOrder("diane@example.com", "eric@example.com")));
    }

    // ================================================================
    // GET /api/v1/accounts/{id}
    // ================================================================

    @Test
    @DisplayName("GET /accounts/{id} — 200 : compte trouvé")
    void getAccountById_found_returns200() throws Exception {
        String id = createAccountAndReturn("Fanny", "fanny@example.com", AccountType.CHECKING);

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.ownerName").value("Fanny"));
    }

    @Test
    @DisplayName("GET /accounts/{id} — 404 : compte inconnu")
    void getAccountById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    // ================================================================
    // POST /api/v1/accounts/{id}/deposit
    // ================================================================

    @Test
    @DisplayName("POST /accounts/{id}/deposit — 200 : solde mis à jour")
    void deposit_validAmount_updatesBalance() throws Exception {
        String id = createAccountAndReturn("Gabriel", "gabriel@example.com", AccountType.SAVINGS);
        TransactionRequest depositReq = buildTransactionRequest(new BigDecimal("500.00"), "Virement");

        mockMvc.perform(post(BASE_URL + "/" + id + "/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(depositReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.00));
    }

    @Test
    @DisplayName("POST /accounts/{id}/deposit — 400 : montant à zéro")
    void deposit_zeroAmount_returns400() throws Exception {
        String id = createAccountAndReturn("Hélène", "helene@example.com", AccountType.CHECKING);
        TransactionRequest req = buildTransactionRequest(BigDecimal.ZERO, null);

        mockMvc.perform(post(BASE_URL + "/" + id + "/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ================================================================
    // POST /api/v1/accounts/{id}/withdraw
    // ================================================================

    @Test
    @DisplayName("POST /accounts/{id}/withdraw — 200 : retrait réussi")
    void withdraw_sufficientFunds_updatesBalance() throws Exception {
        String id = createAccountAndReturn("Igor", "igor@example.com", AccountType.CHECKING);
        // Dépôt préalable
        TransactionRequest depositReq = buildTransactionRequest(new BigDecimal("1000.00"), "Initial");
        mockMvc.perform(post(BASE_URL + "/" + id + "/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(depositReq)));

        // Retrait
        TransactionRequest withdrawReq = buildTransactionRequest(new BigDecimal("250.00"), "Retrait CB");
        mockMvc.perform(post(BASE_URL + "/" + id + "/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(withdrawReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(750.00));
    }

    @Test
    @DisplayName("POST /accounts/{id}/withdraw — 400 : solde insuffisant")
    void withdraw_insufficientFunds_returns400() throws Exception {
        String id = createAccountAndReturn("Julie", "julie@example.com", AccountType.SAVINGS);
        // Aucun dépôt — solde = 0
        TransactionRequest req = buildTransactionRequest(new BigDecimal("500.00"), "Retrait");

        mockMvc.perform(post(BASE_URL + "/" + id + "/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
    }

    // ================================================================
    // GET /api/v1/accounts/{id}/transactions
    // ================================================================

    @Test
    @DisplayName("GET /accounts/{id}/transactions — 200 : historique paginé")
    void getTransactionHistory_afterOperations_returnsPaginatedHistory() throws Exception {
        String id = createAccountAndReturn("Kevin", "kevin@example.com", AccountType.CHECKING);

        // Effectuer 2 dépôts
        TransactionRequest dep1 = buildTransactionRequest(new BigDecimal("100.00"), "Dep 1");
        TransactionRequest dep2 = buildTransactionRequest(new BigDecimal("200.00"), "Dep 2");
        mockMvc.perform(post(BASE_URL + "/" + id + "/deposit")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(dep1)));
        mockMvc.perform(post(BASE_URL + "/" + id + "/deposit")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(dep2)));

        // Consulter l'historique
        mockMvc.perform(get(BASE_URL + "/" + id + "/transactions")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].type").value("CREDIT"))
                .andExpect(jsonPath("$.content[0].accountId").value(id))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /accounts/{id}/transactions — 404 : compte inconnu")
    void getTransactionHistory_unknownAccount_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID() + "/transactions"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    // ================================================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // ================================================================

    private AccountRequest buildAccountRequest(String ownerName, String email, AccountType type) {
        AccountRequest req = new AccountRequest();
        req.setOwnerName(ownerName);
        req.setEmail(email);
        req.setAccountType(type);
        req.setCurrency("EUR");
        return req;
    }

    private TransactionRequest buildTransactionRequest(BigDecimal amount, String description) {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(amount);
        req.setDescription(description);
        return req;
    }

    /**
     * Crée un compte via l'API et retourne son UUID sous forme de String.
     */
    private String createAccountAndReturn(String ownerName, String email, AccountType type)
            throws Exception {
        AccountRequest request = buildAccountRequest(ownerName, email, type);
        MvcResult result = mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }
}
