package com.bank.service;

import com.bank.dto.AccountRequest;
import com.bank.dto.AccountResponse;
import com.bank.dto.TransactionRequest;
import com.bank.dto.TransactionResponse;
import com.bank.exception.AccountNotActiveException;
import com.bank.exception.AccountNotFoundException;
import com.bank.exception.EmailAlreadyExistsException;
import com.bank.exception.InsufficientFundsException;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service métier principal pour la gestion des comptes bancaires.
 *
 * <p>
 * Toutes les opérations financières (dépôt, retrait) sont annotées
 * {@code @Transactional} pour garantir les propriétés ACID : si une étape
 * échoue, le rollback est automatique et aucune donnée incohérente n'est
 * persistée.
 * </p>
 *
 * <p>
 * Les opérations en lecture seule utilisent
 * {@code @Transactional(readOnly = true)} pour permettre à Hibernate
 * d'optimiser le flush et à la BDD d'activer des verrous partagés.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // ============================================================
    // CRÉER UN COMPTE
    // ============================================================

    /**
     * Crée un nouveau compte bancaire après validation de l'unicité de l'email.
     *
     * @param request les données de création validées par {@code @Valid}
     * @return le DTO du compte créé
     * @throws EmailAlreadyExistsException si l'email est déjà enregistré
     */
    @Transactional
    public AccountResponse createAccount(AccountRequest request) {
        log.info("Création de compte pour : {}", request.getEmail());

        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        Account account = Account.builder()
                .ownerName(request.getOwnerName())
                .email(request.getEmail())
                .accountType(request.getAccountType())
                .currency(request.getCurrency() != null && !request.getCurrency().isBlank()
                        ? request.getCurrency().toUpperCase()
                        : "EUR")
                .status(Account.AccountStatus.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);
        log.info("Compte créé avec succès : numéro={}", saved.getAccountNumber());
        return AccountResponse.from(saved);
    }

    // ============================================================
    // LISTE DE TOUS LES COMPTES
    // ============================================================

    /**
     * Retourne la liste complète des comptes.
     *
     * @return liste des DTO AccountResponse
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        log.info("Récupération de tous les comptes");
        return accountRepository.findAll()
                .stream()
                .map(AccountResponse::from)
                .collect(Collectors.toList());
    }

    // ============================================================
    // DÉTAIL D'UN COMPTE PAR ID
    // ============================================================

    /**
     * Retourne les détails d'un compte spécifique.
     *
     * @param id l'identifiant UUID du compte
     * @return le DTO AccountResponse
     * @throws AccountNotFoundException si introuvable
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID id) {
        log.info("Récupération du compte : id={}", id);
        Account account = findAccountOrThrow(id);
        return AccountResponse.from(account);
    }

    // ============================================================
    // DÉPÔT (CREDIT)
    // ============================================================

    /**
     * Effectue un dépôt sur un compte actif.
     *
     * <p>
     * <strong>Garantie ACID :</strong> La mise à jour du solde ET
     * l'enregistrement de la transaction sont dans la même transaction JPA.
     * </p>
     *
     * @param accountId l'identifiant du compte cible
     * @param request   le montant et les métadonnées du dépôt
     * @return le DTO du compte mis à jour
     * @throws AccountNotFoundException  si le compte est introuvable
     * @throws AccountNotActiveException si le compte n'est pas actif
     */
    @Transactional
    public AccountResponse deposit(UUID accountId, TransactionRequest request) {
        log.info("Dépôt de {} sur le compte {}", request.getAmount(), accountId);

        Account account = findAccountOrThrow(accountId);
        validateAccountIsActive(account);

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        recordTransaction(account, Transaction.TransactionType.CREDIT, request);

        log.info("Dépôt réussi. Nouveau solde : {} {}", account.getBalance(), account.getCurrency());
        return AccountResponse.from(account);
    }

    // ============================================================
    // RETRAIT (DEBIT)
    // ============================================================

    /**
     * Effectue un retrait sur un compte actif avec vérification du solde.
     *
     * <p>
     * La comparaison utilise {@code compareTo} (sémantique numérique) et non
     * {@code equals} (qui tient compte de la précision BigDecimal).
     * </p>
     *
     * @param accountId l'identifiant du compte source
     * @param request   le montant et les métadonnées du retrait
     * @return le DTO du compte mis à jour
     * @throws AccountNotFoundException   si le compte est introuvable
     * @throws AccountNotActiveException  si le compte n'est pas actif
     * @throws InsufficientFundsException si le solde est insuffisant
     */
    @Transactional
    public AccountResponse withdraw(UUID accountId, TransactionRequest request) {
        log.info("Retrait de {} depuis le compte {}", request.getAmount(), accountId);

        Account account = findAccountOrThrow(accountId);
        validateAccountIsActive(account);

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("Solde insuffisant pour le compte {} : solde={}, demande={}",
                    accountId, account.getBalance(), request.getAmount());
            throw new InsufficientFundsException(account.getBalance(), request.getAmount());
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        recordTransaction(account, Transaction.TransactionType.DEBIT, request);

        log.info("Retrait réussi. Nouveau solde : {} {}", account.getBalance(), account.getCurrency());
        return AccountResponse.from(account);
    }

    // ============================================================
    // HISTORIQUE DES TRANSACTIONS (paginé)
    // ============================================================

    /**
     * Retourne l'historique paginé des transactions d'un compte,
     * trié du plus récent au plus ancien.
     *
     * @param accountId l'identifiant du compte
     * @param pageable  les paramètres de pagination (page, size, sort)
     * @return la page de DTOs TransactionResponse
     * @throws AccountNotFoundException si le compte est introuvable
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(UUID accountId, Pageable pageable) {
        findAccountOrThrow(accountId);
        return transactionRepository
                .findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
                .map(TransactionResponse::from);
    }

    // ============================================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // ============================================================

    private Account findAccountOrThrow(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    private void validateAccountIsActive(Account account) {
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(account.getAccountNumber(), account.getStatus());
        }
    }

    private void recordTransaction(Account account,
            Transaction.TransactionType type,
            TransactionRequest request) {
        Transaction transaction = Transaction.builder()
                .account(account)
                .type(type)
                .amount(request.getAmount())
                .balanceAfter(account.getBalance())
                .description(request.getDescription())
                .reference(request.getReference())
                .build();
        transactionRepository.save(transaction);
    }
}