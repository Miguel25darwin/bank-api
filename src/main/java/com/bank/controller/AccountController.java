package com.bank.controller;

import com.bank.dto.AccountRequest;
import com.bank.dto.AccountResponse;
import com.bank.dto.TransactionRequest;
import com.bank.dto.TransactionResponse;
import com.bank.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST pour la gestion des comptes bancaires.
 *
 * <p>
 * Point d'entrée de la Layered Architecture :
 * Reçoit les requêtes HTTP → valide les DTOs → délègue au Service → retourne le
 * résultat.
 * </p>
 *
 * <p>
 * Base URL : {@code /api/v1/accounts}
 * </p>
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Comptes Bancaires", description = "API de gestion des comptes et transactions bancaires")
public class AccountController {

        private final AccountService accountService;

        // ============================================================
        // POST /api/v1/accounts — Créer un compte
        // ============================================================
        @Operation(summary = "Créer un nouveau compte bancaire", description = "Crée un compte après validation de l'unicité de l'email. Retourne 201 Created.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Compte créé avec succès"),
                        @ApiResponse(responseCode = "400", description = "Données de création invalides"),
                        @ApiResponse(responseCode = "409", description = "Email déjà utilisé par un autre compte")
        })
        @PostMapping
        public ResponseEntity<AccountResponse> createAccount(
                        @Valid @RequestBody AccountRequest request) {
                log.info("POST /api/v1/accounts — Création de compte pour : {}", request.getEmail());
                AccountResponse response = accountService.createAccount(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        // ============================================================
        // GET /api/v1/accounts — Lister tous les comptes
        // ============================================================
        @Operation(summary = "Lister tous les comptes bancaires", description = "Retourne l'ensemble des comptes enregistrés en base.")
        @ApiResponse(responseCode = "200", description = "Liste retournée avec succès")
        @GetMapping
        public ResponseEntity<List<AccountResponse>> getAllAccounts() {
                log.info("GET /api/v1/accounts — Récupération de tous les comptes");
                return ResponseEntity.ok(accountService.getAllAccounts());
        }

        // ============================================================
        // GET /api/v1/accounts/{id} — Détail d'un compte
        // ============================================================
        @Operation(summary = "Obtenir un compte par son identifiant UUID", description = "Retourne les détails d'un compte spécifique.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Compte trouvé"),
                        @ApiResponse(responseCode = "404", description = "Compte introuvable")
        })
        @GetMapping("/{id}")
        public ResponseEntity<AccountResponse> getAccountById(
                        @Parameter(description = "UUID du compte") @PathVariable UUID id) {
                log.info("GET /api/v1/accounts/{} — Récupération du compte", id);
                return ResponseEntity.ok(accountService.getAccountById(id));
        }

        // ============================================================
        // POST /api/v1/accounts/{id}/deposit — Dépôt
        // ============================================================
        @Operation(summary = "Effectuer un dépôt (CREDIT)", description = "Crédite le compte du montant spécifié. Op. transactionnelle (ACID).")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Dépôt effectué, solde mis à jour"),
                        @ApiResponse(responseCode = "400", description = "Montant invalide ou compte inactif"),
                        @ApiResponse(responseCode = "404", description = "Compte introuvable")
        })
        @PostMapping("/{id}/deposit")
        public ResponseEntity<AccountResponse> deposit(
                        @Parameter(description = "UUID du compte à créditer") @PathVariable UUID id,
                        @Valid @RequestBody TransactionRequest request) {
                log.info("POST /api/v1/accounts/{}/deposit — Montant : {}", id, request.getAmount());
                return ResponseEntity.ok(accountService.deposit(id, request));
        }

        // ============================================================
        // POST /api/v1/accounts/{id}/withdraw — Retrait
        // ============================================================
        @Operation(summary = "Effectuer un retrait (DEBIT)", description = "Débite le compte du montant spécifié après vérification du solde. Op. transactionnelle (ACID).")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Retrait effectué, solde mis à jour"),
                        @ApiResponse(responseCode = "400", description = "Montant invalide, solde insuffisant ou compte inactif"),
                        @ApiResponse(responseCode = "403", description = "Compte suspendu ou clôturé"),
                        @ApiResponse(responseCode = "404", description = "Compte introuvable")
        })
        @PostMapping("/{id}/withdraw")
        public ResponseEntity<AccountResponse> withdraw(
                        @Parameter(description = "UUID du compte à débiter") @PathVariable UUID id,
                        @Valid @RequestBody TransactionRequest request) {
                log.info("POST /api/v1/accounts/{}/withdraw — Montant : {}", id, request.getAmount());
                return ResponseEntity.ok(accountService.withdraw(id, request));
        }

        // ============================================================
        // GET /api/v1/accounts/{id}/transactions — Historique paginé
        // ============================================================
        @Operation(summary = "Historique des transactions (paginé)", description = "Retourne l'historique des mouvements financiers, du plus récent au plus ancien. "
                        + "Paramètre : ?page=0&size=10")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Historique retourné"),
                        @ApiResponse(responseCode = "404", description = "Compte introuvable")
        })
        @GetMapping("/{id}/transactions")
        public ResponseEntity<Page<TransactionResponse>> getTransactions(
                        @Parameter(description = "UUID du compte") @PathVariable UUID id,
                        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
                log.info("GET /api/v1/accounts/{}/transactions — page={}, size={}",
                                id, pageable.getPageNumber(), pageable.getPageSize());
                return ResponseEntity.ok(accountService.getTransactionHistory(id, pageable));
        }
}
